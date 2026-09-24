package com.example.rule.model;

import lombok.Data;

/**
 * 规则字段元数据：驱动"页面表单渲染"和"后端参数校验"的同一份数据源。
 * 字段类型：STRING / NUMBER / DECIMAL(0~1) / ENUM / CSV(多值列表)
 */
@Data
public class RuleTypeField {

    private String ruleType;
    private String fieldKey;
    /** 中文 label，页面直接显示 */
    private String fieldName;
    private String fieldType;
    private boolean required;
    private String defaultValue;
    private Double minValue;
    private Double maxValue;
    /** ENUM 类型的取值，逗号分隔 */
    private String enumOptions;
    private String placeholder;
    private int sortOrder;

    public static RuleTypeField of(String ruleType, String fieldKey, String fieldName, String fieldType,
                                   boolean required, String defaultValue, String enumOptions,
                                   String placeholder, int sortOrder) {
        RuleTypeField f = new RuleTypeField();
        f.ruleType = ruleType;
        f.fieldKey = fieldKey;
        f.fieldName = fieldName;
        f.fieldType = fieldType;
        f.required = required;
        f.defaultValue = defaultValue;
        f.enumOptions = enumOptions;
        f.placeholder = placeholder;
        f.sortOrder = sortOrder;
        return f;
    }

    public RuleTypeField range(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }
}
