package com.example.rule;

import com.example.rule.catalog.RuleTypeCatalog;
import com.example.rule.drl.DrlGenerator;
import com.example.rule.drl.RuleParamValidator;
import com.example.rule.dt.DecisionTableDrlExporter;
import com.example.rule.dt.DecisionTableGuard;
import com.example.rule.dt.DecisionTableXlsxWriter;
import com.example.rule.engine.DynamicRuleEngine;
import com.example.rule.engine.RuleCompileException;
import com.example.rule.model.DecisionTableSpec;
import com.example.rule.service.DecisionTableAppService;
import com.example.rule.service.RuleAppService;
import com.example.rule.store.RuleRepository;
import com.example.rule.store.RuleSourceLoader;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则引擎闭环测试（不起 Spring 容器，直接 new 手工装配，几秒跑完）。
 * 覆盖：参数模板发布 → 立即生效、拦截规则、脏参数/脏 DRL 被拦、决策表上传发布、决策表篡改被拒。
 */
class RuleEngineLocalTest {

    private RuleAppService ruleService;
    private DecisionTableAppService decisionTableService;
    private RuleTypeCatalog catalog;
    private RuleRepository repository;
    private DynamicRuleEngine engine;

    @BeforeEach
    void setUp() {
        catalog = new RuleTypeCatalog();
        catalog.seed();
        repository = new RuleRepository();
        engine = new DynamicRuleEngine();
        RuleSourceLoader loader = new RuleSourceLoader(repository, engine);
        ruleService = new RuleAppService(catalog, new RuleParamValidator(), new DrlGenerator(), repository, loader, engine);
        ruleService.init();
        decisionTableService = new DecisionTableAppService(repository, catalog, new DecisionTableXlsxWriter(),
                new DecisionTableGuard(), new DecisionTableDrlExporter(), loader);
        decisionTableService.seedDefaultAsset();
    }

    @Test
    @DisplayName("种子规则生效：VIP 9 折、新疆加 15 运费")
    void seedRulesTakeEffect() {
        Map<String, Object> result = ruleService.run("VIP", "新疆", 1000.0, 1);
        assertEquals(0.1, result.get("discount"));
        assertEquals(15.0, result.get("shippingFee"));
        assertEquals(915.0, result.get("finalAmount"));
        assertTrue(result.get("messages").toString().contains("客户等级折扣命中"));
    }

    @Test
    @DisplayName("页面改参数发布后立刻生效，无需重启")
    void publishTakesEffectImmediately() {
        Map<String, String> params = new HashMap<>();
        params.put("level", "GOLD");
        params.put("rate", "0.25");
        Map<String, Object> published = ruleService.publish("VIP_DISCOUNT", params, "gold-2026", "tester");
        assertTrue((int) published.get("ruleCount") >= 4);

        Map<String, Object> result = ruleService.run("GOLD", "北京", 1000.0, 1);
        assertEquals(0.25, result.get("discount"));
        assertEquals(750.0, result.get("finalAmount"));
    }

    @Test
    @DisplayName("重复条件的新规则被拒绝（要改就覆盖同一个业务键）")
    void duplicateConditionRejected() {
        Map<String, String> params = new HashMap<>();
        params.put("level", "VIP");
        params.put("rate", "0.1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ruleService.publish("VIP_DISCOUNT", params, "another-vip", "tester"));
        assertTrue(error.getMessage().contains("已存在相同条件"));

        // 用同一个业务键覆盖则允许，版本 +1
        Map<String, String> updated = new HashMap<>();
        updated.put("level", "VIP");
        updated.put("rate", "0.3");
        Map<String, Object> published = ruleService.publish("VIP_DISCOUNT", updated, "demo-vip", "tester");
        assertEquals(2, published.get("version"));
        assertEquals(0.3, ruleService.run("VIP", "北京", 1000.0, 1).get("discount"));
    }

    @Test
    @DisplayName("拦截规则：件数超限整单被拦，金额按 0 处理")
    void rejectRuleBlocksOrder() {
        Map<String, Object> result = ruleService.run("NORMAL", "北京", 1000.0, 100);
        assertEquals(true, result.get("rejected"));
        assertEquals(0.0, result.get("finalAmount"));
    }

    @Test
    @DisplayName("脏参数被拦下（折扣率 5 超范围），且不影响生效版本")
    void badParamRejected() {
        String versionBefore = engine.activeVersion();
        Map<String, String> params = new HashMap<>();
        params.put("level", "VIP");
        params.put("rate", "5");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ruleService.publish("VIP_DISCOUNT", params, "bad-rule", "tester"));
        assertTrue(error.getMessage().contains("0 到 1"));
        assertEquals(versionBefore, engine.activeVersion());
        assertEquals(0.1, ruleService.run("VIP", "北京", 1000.0, 1).get("discount"));
    }

    @Test
    @DisplayName("模型里查不到的规则类型直接拒绝（新增类型靠配置，不靠改代码）")
    void unknownRuleTypeRejected() {
        assertThrows(IllegalArgumentException.class, () -> ruleService.preview("NOT_EXIST_TYPE", new HashMap<>()));
    }

    @Test
    @DisplayName("决策表上传后生效：BLUE 等级 35% 折扣")
    void decisionTableTakesEffect() {
        DecisionTableSpec spec = repository.asset(DecisionTableAppService.DEFAULT_ASSET_KEY).getSpec();
        byte[] xlsx = new DecisionTableXlsxWriter().write(spec, List.of(
                new String[]{"蓝钻客户", "BLUE", "0.35"},
                new String[]{"金卡客户", "GOLD", "0.18"}));

        Map<String, Object> published = decisionTableService.publish(
                DecisionTableAppService.DEFAULT_ASSET_KEY, xlsx, "tester");
        assertTrue((int) published.get("ruleCount") >= 5);
        assertTrue(String.valueOf(published.get("drl")).contains("BLUE"));

        assertEquals(0.35, ruleService.run("BLUE", "北京", 1000.0, 1).get("discount"));
        assertEquals(0.18, ruleService.run("GOLD", "北京", 1000.0, 1).get("discount"));
    }

    @Test
    @DisplayName("决策表被篡改结构（往 D 列塞动作代码）必须被拒绝")
    void tamperedDecisionTableRejected() throws Exception {
        DecisionTableSpec spec = repository.asset(DecisionTableAppService.DEFAULT_ASSET_KEY).getSpec();
        byte[] xlsx = new DecisionTableXlsxWriter().write(spec, List.<String[]>of(new String[]{"蓝钻客户", "BLUE", "0.35"}));

        byte[] tampered;
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(9);
            row.createCell(3).setCellValue("$o.setDiscount(1); java.lang.Runtime.getRuntime().exec(\"id\");");
            workbook.write(out);
            tampered = out.toByteArray();
        }

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> decisionTableService.publish(DecisionTableAppService.DEFAULT_ASSET_KEY, tampered, "hacker"));
        assertTrue(error.getMessage().contains("不允许填写"));
        // 篡改的内容没有进引擎：该等级仍然没有折扣
        assertEquals(0.0, ruleService.run("BLUE", "北京", 1000.0, 1).get("discount"));
    }

    @Test
    @DisplayName("决策表取值超出白名单 / 公式单元格都必须被拒绝")
    void decisionTableValueAndFormulaRejected() throws Exception {
        DecisionTableSpec spec = repository.asset(DecisionTableAppService.DEFAULT_ASSET_KEY).getSpec();
        byte[] xlsx = new DecisionTableXlsxWriter().write(spec, List.<String[]>of(new String[]{"野生等级", "SUPER_HACKER", "0.99"}));
        assertThrows(IllegalArgumentException.class,
                () -> decisionTableService.publish(DecisionTableAppService.DEFAULT_ASSET_KEY, xlsx, "tester"));

        byte[] withFormula;
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(9);
            row.getCell(0).setCellValue("正常名字");
            row.getCell(1).setCellValue("VIP");
            row.getCell(2).setCellFormula("1+1");
            workbook.write(out);
            withFormula = out.toByteArray();
        }
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> decisionTableService.publish(DecisionTableAppService.DEFAULT_ASSET_KEY, withFormula, "tester"));
        assertTrue(error.getMessage().contains("公式"));
    }

    @Test
    @DisplayName("停用规则后效果立刻消失，启用后回来")
    void disableAndEnableRule() {
        long id = repository.all().get(0).getId();
        ruleService.changeStatus(id, 2);
        assertEquals(0.0, ruleService.run("VIP", "北京", 1000.0, 1).get("discount"));
        ruleService.changeStatus(id, 1);
        assertEquals(0.1, ruleService.run("VIP", "北京", 1000.0, 1).get("discount"));
    }

    @Test
    @DisplayName("编译器级错误（DRL 语法坏）也要在发布环节被拦住")
    void brokenDrlRejected() {
        RuleSourceLoader loader = new RuleSourceLoader(repository, engine);
        assertThrows(RuleCompileException.class, () -> loader.dryRun(
                Map.of("BAD_RULE", "package x;\ndialect \"mvel\"\nrule \"bad\" when this is not java end"), null));
        assertFalse(engine.activeVersion().isEmpty());
    }
}
