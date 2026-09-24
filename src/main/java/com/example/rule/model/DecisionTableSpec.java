package com.example.rule.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 决策表的"结构契约"：页面下载的模板、上传校验、DRL 生成都以它为准。
 * 关键安全点：上传的 xlsx 只允许在这套结构里改"取值列"，
 * 因为决策表单元格会被编译成规则 RHS（= 任意 Java 代码），不锁结构等于允许代码注入。
 */
@Data
public class DecisionTableSpec {

    private String ruleSetPackage = "com.example.rule.generated";
    private String importClass = "com.example.rule.model.Order";
    private String tableName;
    /** 条件列模式，如 "$o : Order" */
    private String pattern = "$o : Order";
    /** 条件取值模板，如 customerLevel == "$param" */
    private String conditionTemplate;
    /** 动作模板，如 $o.setDiscount($param); update($o); */
    private String actionTemplate;
    private String conditionLabel;
    private String actionLabel;
    /** 条件列允许的取值白名单（空=按 valuePattern 校验） */
    private List<String> conditionWhitelist = new ArrayList<>();
    /** 条件取值列的正则（白名单为空时生效） */
    private String valuePattern = "^[A-Z_]{2,16}$";
    /** 动作取值列正则：折扣率 0~1 或整数金额 */
    private String actionPattern = "^(0(\\.\\d{1,4})?|1(\\.0{1,4})?|\\d{1,6}(\\.\\d{1,4})?)$";
}
