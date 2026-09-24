package com.example.rule.drl;

import com.example.rule.model.RuleTypeField;
import com.example.rule.model.RuleTypeMeta;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 参数校验器。与 RuleTypeCatalog 的字段元数据同源：
 * 页面渲染表单用它，后端校验也用它 —— 一份数据，不会出现"页面能填、后端不认"的错位。
 */
@Component
public class RuleParamValidator {

    private static final int MAX_CSV_ITEMS = 20;
    private static final int MAX_STRING_LEN = 50;

    /** 校验并归一化（缺失的必填项如果有默认值就补上），返回可直接渲染的参数字典。 */
    public Map<String, String> validate(RuleTypeMeta meta, Map<String, String> input) {
        Map<String, String> safe = input == null ? new LinkedHashMap<>() : input;
        Map<String, String> normalized = new LinkedHashMap<>();
        for (RuleTypeField field : meta.getFields()) {
            String value = trim(safe.get(field.getFieldKey()));
            if (value == null || value.isEmpty()) {
                value = field.getDefaultValue();
            }
            if ((value == null || value.isEmpty()) && field.isRequired()) {
                throw new IllegalArgumentException("参数[" + field.getFieldName() + "]必填");
            }
            if (value == null || value.isEmpty()) {
                continue;
            }
            checkType(field, value);
            normalized.put(field.getFieldKey(), value.trim());
        }
        return normalized;
    }

    private void checkType(RuleTypeField field, String value) {
        switch (field.getFieldType()) {
            case "NUMBER" -> checkNumber(field, value);
            case "DECIMAL" -> checkDecimal(field, value);
            case "ENUM" -> checkEnum(field, value);
            case "CSV" -> checkCsv(field, value);
            case "STRING" -> {
                if (value.length() > MAX_STRING_LEN) {
                    throw new IllegalArgumentException("参数[" + field.getFieldName() + "]长度不能超过 " + MAX_STRING_LEN);
                }
            }
            default -> throw new IllegalArgumentException("不支持的字段类型: " + field.getFieldType());
        }
    }

    private void checkNumber(RuleTypeField field, String value) {
        double d = parseDouble(field, value);
        checkRange(field, d);
    }

    private void checkDecimal(RuleTypeField field, String value) {
        double d = parseDouble(field, value);
        if (d < 0 || d > 1) {
            throw new IllegalArgumentException("参数[" + field.getFieldName() + "]必须在 0 到 1 之间，如 0.15 表示 85 折");
        }
        checkRange(field, d);
    }

    private void checkRange(RuleTypeField field, double d) {
        if (field.getMinValue() != null && d < field.getMinValue()) {
            throw new IllegalArgumentException("参数[" + field.getFieldName() + "]不能小于 " + field.getMinValue());
        }
        if (field.getMaxValue() != null && d > field.getMaxValue()) {
            throw new IllegalArgumentException("参数[" + field.getFieldName() + "]不能大于 " + field.getMaxValue());
        }
    }

    private void checkEnum(RuleTypeField field, String value) {
        String options = field.getEnumOptions();
        if (options == null || options.isEmpty()) {
            return;
        }
        for (String option : options.split(",")) {
            if (option.trim().equals(value)) {
                return;
            }
        }
        throw new IllegalArgumentException("参数[" + field.getFieldName() + "]必须是以下之一: " + options);
    }

    private void checkCsv(RuleTypeField field, String value) {
        int count = 0;
        for (String part : value.split(",")) {
            String v = part.trim();
            if (v.isEmpty()) {
                continue;
            }
            count++;
            if (v.length() > MAX_STRING_LEN) {
                throw new IllegalArgumentException("参数[" + field.getFieldName() + "]中的值过长: " + v);
            }
        }
        if (count == 0) {
            throw new IllegalArgumentException("参数[" + field.getFieldName() + "]至少填一个值");
        }
        if (count > MAX_CSV_ITEMS) {
            throw new IllegalArgumentException("参数[" + field.getFieldName() + "]最多 " + MAX_CSV_ITEMS + " 个值");
        }
    }

    private double parseDouble(RuleTypeField field, String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数[" + field.getFieldName() + "]必须是数字，当前值: " + value);
        }
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
