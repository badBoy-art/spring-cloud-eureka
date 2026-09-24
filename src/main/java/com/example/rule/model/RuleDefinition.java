package com.example.rule.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 一条已配置的规则实例（页面发布出来的东西）。 */
@Data
public class RuleDefinition {

    private long id;
    private String ruleGroup;
    private String ruleType;
    /** 业务键：同键重复发布=覆盖（版本+1） */
    private String ruleKey;
    /** DRL 里的 rule 名，全局唯一 */
    private String ruleName;
    private Map<String, String> params = new LinkedHashMap<>();
    /** 渲染后的 DRL 全文（排查用；页面也能直接看） */
    private String drlContent;
    /** 0 草稿 / 1 已发布 / 2 已停用 */
    private int status;
    private int version;
    private String updatedBy;
    private String updatedAt;
    /** 运行期提示（命中说明模板） */
    private String hitMessage;
}
