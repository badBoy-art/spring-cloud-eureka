package com.example.rule.service;

import com.example.rule.catalog.RuleTypeCatalog;
import com.example.rule.drl.DrlGenerator;
import com.example.rule.drl.RuleParamValidator;
import com.example.rule.engine.DynamicRuleEngine;
import com.example.rule.engine.RuleFireResult;
import com.example.rule.model.Order;
import com.example.rule.model.RuleDefinition;
import com.example.rule.model.RuleTypeMeta;
import com.example.rule.store.RuleRepository;
import com.example.rule.store.RuleSourceLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则定义的服务层：页面的"选类型 → 填参数 → 预览 → 试编译 → 发布 → 启停/删除 → 试算"全在这里。
 * Controller 只做转发，业务规则（校验/渲染/编译/换版顺序）都在这一层。
 */
@Service
public class RuleAppService {

    private final RuleTypeCatalog catalog;
    private final RuleParamValidator validator;
    private final DrlGenerator generator;
    private final RuleRepository repository;
    private final RuleSourceLoader sourceLoader;
    private final DynamicRuleEngine engine;

    public RuleAppService(RuleTypeCatalog catalog, RuleParamValidator validator, DrlGenerator generator,
                          RuleRepository repository, RuleSourceLoader sourceLoader, DynamicRuleEngine engine) {
        this.catalog = catalog;
        this.validator = validator;
        this.generator = generator;
        this.repository = repository;
        this.sourceLoader = sourceLoader;
        this.engine = engine;
    }

    /** 启动即生效：先种三条演示规则，再编译一次作为初始版本。 */
    @PostConstruct
    public void init() {
        seedDemoRules();
        refresh();
    }

    public List<RuleTypeMeta> types() {
        return catalog.all();
    }

    /** 预览：只渲染 DRL，不编译（页面右边那块实时预览）。 */
    public Map<String, Object> preview(String ruleType, Map<String, String> params) {
        RuleTypeMeta meta = catalog.byType(ruleType);
        Map<String, String> normalized = validator.validate(meta, params);
        String drl = generator.generate(meta, ruleName(ruleType, "preview"), normalized);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleType", ruleType);
        result.put("params", normalized);
        result.put("drl", drl);
        return result;
    }

    /** 试编译：把候选规则和现有生效规则一起编译一遍，不落库、不换版。 */
    public Map<String, Object> validate(String ruleType, Map<String, String> params) {
        RuleTypeMeta meta = catalog.byType(ruleType);
        Map<String, String> normalized = validator.validate(meta, params);
        String ruleName = ruleName(ruleType, "validate");
        String drl = generator.generate(meta, ruleName, normalized);
        sourceLoader.dryRun(Map.of(ruleName, drl), null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("drl", drl);
        return result;
    }

    /**
     * 发布：校验参数 → 渲染 DRL → 连同现有规则一起编译 → 编译通过才落库。
     * 编译失败时抛 RuleCompileException，仓库和生效版本都不变（脏规则进不了生产）。
     */
    public Map<String, Object> publish(String ruleType, Map<String, String> params, String ruleKey, String updatedBy) {
        RuleTypeMeta meta = catalog.byType(ruleType);
        Map<String, String> normalized = validator.validate(meta, params);
        String key = (ruleKey == null || ruleKey.isBlank()) ? ruleType + "-default" : ruleKey.trim();
        RuleDefinition candidate = buildDefinition(meta, key, normalized, updatedBy);
        checkDuplicateCondition(candidate);

        Map<String, Object> state = sourceLoader.publish(repository.versionTag(),
                Map.of(candidate.getRuleName(), candidate.getDrlContent()), null);
        RuleDefinition saved = repository.publish(candidate);

        Map<String, Object> result = definitionView(saved);
        result.putAll(state);
        return result;
    }

    /**
     * 同类型 + 同参数 = 重复规则，直接拒绝。
     * 否则两条规则的 LHS 完全一样，谁先点火由引擎内部顺序决定，结果不可预期
     * （要改条件就覆盖同一个业务键，而不是新建一条）。
     */
    private void checkDuplicateCondition(RuleDefinition candidate) {
        for (RuleDefinition other : repository.published()) {
            if (!other.getRuleKey().equals(candidate.getRuleKey())
                    && other.getRuleType().equals(candidate.getRuleType())
                    && other.getParams().equals(candidate.getParams())) {
                throw new IllegalArgumentException("已存在相同条件的规则[" + other.getRuleKey()
                        + "]，请直接覆盖该规则（用相同的业务键），不要新建重复规则");
            }
        }
    }

    public List<Map<String, Object>> definitions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (RuleDefinition definition : repository.all()) {
            list.add(definitionView(definition));
        }
        return list;
    }

    /** 启停：先改状态再刷新；刷新失败（比如启用后与现有规则冲突）要把状态改回去。 */
    public Map<String, Object> changeStatus(long id, int status) {
        RuleDefinition definition = repository.requireById(id);
        int original = definition.getStatus();
        repository.changeStatus(id, status);
        try {
            return refresh();
        } catch (RuntimeException e) {
            repository.changeStatus(id, original);
            refresh();
            throw e;
        }
    }

    public Map<String, Object> remove(long id) {
        repository.remove(id);
        return refresh();
    }

    public Map<String, Object> refresh() {
        Map<String, Object> state = sourceLoader.publish(repository.versionTag(), null, null);
        state.put("publishedRules", repository.published().size());
        state.put("totalRules", repository.all().size());
        state.put("assets", repository.allAssets().size());
        return state;
    }

    public Map<String, Object> state() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("activeVersion", engine.activeVersion());
        state.put("ruleCount", engine.ruleCount());
        state.put("lastFireCount", engine.lastFireCount());
        state.put("publishedRules", repository.published().size());
        state.put("totalRules", repository.all().size());
        state.put("assets", repository.allAssets().size());
        return state;
    }

    /** 试算：拿当前生效的规则跑一单，返回结果 + 命中说明（文案由服务层生成，规则 RHS 里不放文案）。 */
    public Map<String, Object> run(String customerLevel, String region, Double amount, Integer itemCount) {
        Order order = new Order(
                customerLevel == null || customerLevel.isBlank() ? "NORMAL" : customerLevel,
                region == null || region.isBlank() ? "北京" : region,
                amount == null ? 1000.0 : amount,
                itemCount == null ? 1 : itemCount);
        RuleFireResult fireResult = engine.fire(order);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("firedRules", fireResult.firedRules());
        result.put("firedCount", fireResult.firedCount());
        result.putAll(orderView(fireResult.order()));
        result.put("messages", hitMessages(fireResult));
        return result;
    }

    private Map<String, Object> orderView(Order order) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("customerLevel", order.getCustomerLevel());
        view.put("region", order.getRegion());
        view.put("amount", order.getAmount());
        view.put("itemCount", order.getItemCount());
        view.put("discount", order.getDiscount());
        view.put("shippingFee", order.getShippingFee());
        view.put("rejected", order.isRejected());
        view.put("bigOrder", order.isBigOrder());
        view.put("finalAmount", order.getFinalAmount());
        return view;
    }

    private List<String> hitMessages(RuleFireResult fireResult) {
        List<String> messages = new ArrayList<>();
        for (String firedRule : fireResult.firedRules()) {
            RuleDefinition definition = repository.findByRuleName(firedRule);
            messages.add(definition != null && definition.getHitMessage() != null
                    ? definition.getHitMessage()
                    : "命中规则[" + firedRule + "]");
        }
        if (messages.isEmpty()) {
            messages.add("未命中任何规则，按原价结算");
        }
        if (fireResult.order().isRejected()) {
            messages.add("订单已被拦截，应付金额按 0 处理");
        }
        return messages;
    }

    private RuleDefinition buildDefinition(RuleTypeMeta meta, String ruleKey, Map<String, String> params, String updatedBy) {
        RuleDefinition definition = new RuleDefinition();
        definition.setRuleGroup(meta.getRuleGroup());
        definition.setRuleType(meta.getRuleType());
        definition.setRuleKey(ruleKey);
        definition.setRuleName(ruleName(meta.getRuleType(), ruleKey));
        definition.setParams(params);
        definition.setDrlContent(generator.generate(meta, definition.getRuleName(), params));
        definition.setStatus(1);
        definition.setUpdatedBy(updatedBy == null || updatedBy.isBlank() ? "admin" : updatedBy);
        definition.setHitMessage(renderTemplate(meta.getHitMessageTemplate(), params, ruleKey));
        return definition;
    }

    /** 规则名 = 类型 + 业务键（去非法字符），同时用作 DRL 文件名，保证唯一且可读。 */
    private String ruleName(String ruleType, String ruleKey) {
        return ruleType + "_" + ruleKey.replaceAll("[^0-9A-Za-z\\u4e00-\\u9fa5_-]", "_");
    }

    private String renderTemplate(String template, Map<String, String> params, String fallback) {
        if (template == null) {
            return "命中规则[" + fallback + "]";
        }
        String text = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            text = text.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    private Map<String, Object> definitionView(RuleDefinition definition) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", definition.getId());
        view.put("ruleType", definition.getRuleType());
        view.put("ruleGroup", definition.getRuleGroup());
        view.put("ruleKey", definition.getRuleKey());
        view.put("ruleName", definition.getRuleName());
        view.put("params", definition.getParams());
        view.put("drl", definition.getDrlContent());
        view.put("status", definition.getStatus());
        view.put("version", definition.getVersion());
        view.put("hitMessage", definition.getHitMessage());
        view.put("updatedBy", definition.getUpdatedBy());
        view.put("updatedAt", definition.getUpdatedAt());
        return view;
    }

    /** 种子数据：三条演示规则，页面一打开就能试算。 */
    private void seedDemoRules() {
        if (!repository.all().isEmpty()) {
            return;
        }
        seed("VIP_DISCOUNT", "demo-vip", Map.of("level", "VIP", "rate", "0.1"));
        seed("REGION_SURCHARGE", "demo-region", Map.of("regions", "新疆,西藏", "fee", "15"));
        seed("STOCK_CHECK", "demo-stock", Map.of("maxItems", "60"));
    }

    private void seed(String ruleType, String ruleKey, Map<String, String> params) {
        RuleDefinition definition = buildDefinition(catalog.byType(ruleType), ruleKey, params, "system-seed");
        repository.publish(definition);
    }
}
