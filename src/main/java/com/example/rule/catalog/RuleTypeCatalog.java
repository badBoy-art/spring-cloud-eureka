package com.example.rule.catalog;

import com.example.rule.model.DecisionTableSpec;
import com.example.rule.model.RuleTypeField;
import com.example.rule.model.RuleTypeMeta;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则类型目录（企业里这份数据应该放在 rule_type_meta / rule_type_field / rule_template 三张表里，
 * 这里是内存种子数据，换成 Dao 即可 —— 新增规则类型零 Java 改动）。
 *
 * 模板统一遵守两条纪律（都是实测踩出来的）：
 *  1) RHS 只调用 setter，不调用别的方法（mvel 方言下 RHS 里出现非 setter 调用会让 update() 退化成
 *     全量更新 → 同一条规则反复点火；提示文案交给服务层生成）。
 *  2) 累加型规则都带状态守卫（discount == 0.0 / shippingFee == 0.0），天然幂等。
 */
@Component
public class RuleTypeCatalog {

    private final Map<String, RuleTypeMeta> types = new LinkedHashMap<>();

    @PostConstruct
    public void seed() {
        types.put("VIP_DISCOUNT", vipDiscount());
        types.put("REGION_SURCHARGE", regionSurcharge());
        types.put("STOCK_CHECK", stockCheck());
        types.put("BIG_ORDER_TAG", bigOrderTag());
    }

    public List<RuleTypeMeta> all() {
        return new ArrayList<>(types.values());
    }

    public RuleTypeMeta byType(String ruleType) {
        RuleTypeMeta meta = types.get(ruleType);
        if (meta == null) {
            throw new IllegalArgumentException("未知规则类型: " + ruleType + "（可用: " + types.keySet() + "）");
        }
        return meta;
    }

    private RuleTypeMeta vipDiscount() {
        RuleTypeMeta m = new RuleTypeMeta();
        m.setRuleType("VIP_DISCOUNT");
        m.setRuleGroup("price");
        m.setTypeName("客户等级折扣");
        m.setTypeDesc("按客户等级给整单折扣，只对未被拦截的订单生效");
        m.setSortOrder(1);
        m.setHitMessageTemplate("客户等级折扣命中：等级[${level}] → 折扣率 ${rate}");
        m.setTemplateBody("""
                    when
                        $o : Order( rejected == false, customerLevel == "${level}", discount == 0.0 )
                    then
                        $o.setDiscount(${rate});
                        update($o);
                """);
        m.getFields().add(RuleTypeField.of("VIP_DISCOUNT", "level", "客户等级", "ENUM", true,
                "VIP", "VIP,GOLD,NORMAL", "选择客户等级", 1));
        m.getFields().add(RuleTypeField.of("VIP_DISCOUNT", "rate", "折扣率", "DECIMAL", true,
                "0.05", null, "0~1 之间，如 0.15 表示 85 折", 2).range(0, 1));
        return m;
    }

    private RuleTypeMeta regionSurcharge() {
        RuleTypeMeta m = new RuleTypeMeta();
        m.setRuleType("REGION_SURCHARGE");
        m.setRuleGroup("region");
        m.setTypeName("偏远地区加收运费");
        m.setTypeDesc("命中区域列表时加收固定运费，只加一次");
        m.setSortOrder(2);
        m.setHitMessageTemplate("偏远地区加收运费命中：区域[${regions}] → 加收 ${fee} 元");
        m.setTemplateBody("""
                    when
                        $o : Order( rejected == false, region in (${regions}), shippingFee == 0.0 )
                    then
                        $o.setShippingFee(${fee});
                        update($o);
                """);
        m.getFields().add(RuleTypeField.of("REGION_SURCHARGE", "regions", "区域列表", "CSV", true,
                "新疆,西藏", null, "逗号分隔，如 新疆,西藏,青海", 1));
        m.getFields().add(RuleTypeField.of("REGION_SURCHARGE", "fee", "加收运费(元)", "NUMBER", true,
                "20", null, "大于等于 0", 2).range(0, 10000));
        return m;
    }

    private RuleTypeMeta stockCheck() {
        RuleTypeMeta m = new RuleTypeMeta();
        m.setRuleType("STOCK_CHECK");
        m.setRuleGroup("inventory");
        m.setTypeName("件数超限拦截");
        m.setTypeDesc("单笔件数超过阈值直接拦截该订单（salience 最高，先于折扣类规则生效）");
        m.setSortOrder(3);
        m.setHitMessageTemplate("件数超限拦截命中：件数超过 ${maxItems}，整单被拦截");
        m.setTemplateBody("""
                    when
                        $o : Order( rejected == false, itemCount > ${maxItems} )
                    then
                        $o.setRejected(true);
                        update($o);
                """);
        m.getFields().add(RuleTypeField.of("STOCK_CHECK", "maxItems", "件数阈值", "NUMBER", true,
                "50", null, "超过该件数即拦截", 1).range(1, 100000));
        return m;
    }

    private RuleTypeMeta bigOrderTag() {
        RuleTypeMeta m = new RuleTypeMeta();
        m.setRuleType("BIG_ORDER_TAG");
        m.setRuleGroup("tag");
        m.setTypeName("大额订单标记");
        m.setTypeDesc("订单金额达到阈值时打标，供后续风控/审核流程使用");
        m.setSortOrder(4);
        m.setHitMessageTemplate("大额订单标记命中：金额达到 ${threshold} 元，已打标待审核");
        m.setTemplateBody("""
                    when
                        $o : Order( rejected == false, bigOrder == false, amount >= ${threshold} )
                    then
                        $o.setBigOrder(true);
                        update($o);
                """);
        m.getFields().add(RuleTypeField.of("BIG_ORDER_TAG", "threshold", "金额阈值(元)", "NUMBER", true,
                "5000", null, "订单金额达到该值即打标", 1).range(0, 100000000));
        return m;
    }

    /** 决策表资产的默认结构契约（页面下载的模板就是按它生成的）。 */
    public DecisionTableSpec defaultDecisionTableSpec() {
        DecisionTableSpec spec = new DecisionTableSpec();
        spec.setTableName("订单折扣决策表");
        spec.setConditionTemplate("customerLevel == \"$param\"");
        spec.setActionTemplate("$o.setDiscount($param); update($o);");
        spec.setConditionLabel("客户等级");
        spec.setActionLabel("折扣率");
        spec.setConditionWhitelist(List.of("VIP", "GOLD", "NORMAL", "BLUE"));
        return spec;
    }
}
