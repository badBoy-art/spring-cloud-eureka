package com.example.rule.model;

import lombok.Data;

/** 决策表资产：一份 xlsx 字节流 + 它的结构契约 + 生效版本。 */
@Data
public class RuleAsset {

    private String assetKey;
    private String assetName;
    private DecisionTableSpec spec;
    /** 原始 xlsx 字节（页面下载/上传的对象） */
    private byte[] xlsx;
    /** 编译后导出的 DRL（DRL 版决策表，排查用） */
    private String drlContent;
    private int version;
    /** 0 草稿 / 1 已发布 / 2 已停用 */
    private int status;
    private String updatedBy;
    private String updatedAt;
}
