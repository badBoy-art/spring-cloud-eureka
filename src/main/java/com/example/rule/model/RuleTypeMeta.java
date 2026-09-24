package com.example.rule.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则类型元数据 = "这种规则是什么 + 需要哪些参数 + 怎么渲染成 DRL"。
 * 页面拿它渲染表单，后端拿它校验参数、渲染 DRL —— 一份数据三处复用。
 */
@Data
public class RuleTypeMeta {

    private String ruleType;
    /** 分组：price / region / inventory / tag */
    private String ruleGroup;
    private String typeName;
    private String typeDesc;
    private int sortOrder;
    /** DRL 片段模板，when/then 中带 ${fieldKey} 占位符 */
    private String templateBody;
    /** 命中说明模板（页面发布后展示、运行期回显），同样支持 ${fieldKey} */
    private String hitMessageTemplate;
    private List<RuleTypeField> fields = new ArrayList<>();
}
