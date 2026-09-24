package com.example.rule.drl;

import com.example.rule.model.RuleTypeField;
import com.example.rule.model.RuleTypeMeta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把"页面填的参数"渲染成 DRL。
 * 安全边界：页面永远改不到 when/then 的结构，只能替换模板里的 ${fieldKey} 取值 ——
 * 这是"规则页面化"能安全落地的前提（决策表那条路要靠 DecisionTableGuard 做同等约束）。
 */
@Component
public class DrlGenerator {

    public static final String PACKAGE_NAME = "com.example.rule.generated";

    public String generate(RuleTypeMeta meta, String ruleName, Map<String, String> params) {
        String body = meta.getTemplateBody();
        for (RuleTypeField field : meta.getFields()) {
            String value = render(field, params.get(field.getFieldKey()));
            body = body.replace("${" + field.getFieldKey() + "}", value);
        }
        if (body.contains("${")) {
            throw new IllegalStateException("模板存在未替换的占位符，请检查 rule_type_field 是否缺字段: " + meta.getRuleType());
        }
        return header() + "rule \"" + ruleName + "\"\n" + body + "end\n";
    }

    public String header() {
        return "package " + PACKAGE_NAME + ";\n"
                + "dialect \"mvel\"\n"
                + "import com.example.rule.model.Order\n\n";
    }

    private String render(RuleTypeField field, String raw) {
        String value = raw == null ? field.getDefaultValue() : raw;
        if (value == null) {
            throw new IllegalStateException("字段[" + field.getFieldName() + "]取值为空");
        }
        if ("CSV".equals(field.getFieldType())) {
            return toQuotedList(value);
        }
        return value.trim();
    }

    /** CSV -> "新疆", "西藏" （DRL 里 in (...) 的写法） */
    private String toQuotedList(String csv) {
        List<String> items = new ArrayList<>();
        for (String part : csv.split(",")) {
            String v = part.trim();
            if (!v.isEmpty()) {
                items.add("\"" + v.replace("\"", "") + "\"");
            }
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("多值字段解析后为空");
        }
        return String.join(", ", items);
    }
}
