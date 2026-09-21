package com.example.asr;

import lombok.Data;

/**
 * 一次语音识别的结果。识别失败不抛异常，统一返回 degraded=true，由上层决定降级 UX。
 *
 * @author badBoy
 */
@Data
public class AsrResult {

    /** 识别文本；降级时为空串 */
    private String text;

    /** 是否降级（超时 / 无空闲连接 / 开关关闭 / 音频无效） */
    private boolean degraded;

    /** 降级原因，用于埋点与告警归类 */
    private String degradeReason;

    /** 端到端耗时（毫秒） */
    private long costMs;

    /** 实际命中的实例地址，便于定位某台机器异常 */
    private String endpoint;

    public static AsrResult ok(String text, long costMs, String endpoint) {
        AsrResult result = new AsrResult();
        result.setText(text);
        result.setDegraded(false);
        result.setCostMs(costMs);
        result.setEndpoint(endpoint);
        return result;
    }

    public static AsrResult degrade(String reason, long costMs) {
        AsrResult result = new AsrResult();
        result.setText("");
        result.setDegraded(true);
        result.setDegradeReason(reason);
        result.setCostMs(costMs);
        return result;
    }
}
