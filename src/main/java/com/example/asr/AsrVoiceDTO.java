package com.example.asr;

import lombok.Data;

/**
 * 语音识别接口的返回体。
 *
 * @author badBoy
 */
@Data
public class AsrVoiceDTO {

    /** 原始识别文本 */
    private String text;

    /** 清洗/归一/别名纠错后的文本，可直接作为检索词；空串表示没有可用文本 */
    private String normalizedText;

    /** 是否降级（超时/无空闲连接/开关关闭/音频无效） */
    private boolean degraded;

    /** 降级原因 */
    private String degradeReason;

    /** 端到端耗时（毫秒） */
    private long costMs;

    /** 命中的 FunASR 实例 */
    private String endpoint;

    /** 音频字节数，便于排查前端采样格式问题 */
    private int audioBytes;

    public static AsrVoiceDTO of(AsrResult result, String normalizedText, int audioBytes) {
        AsrVoiceDTO dto = new AsrVoiceDTO();
        dto.setText(result.getText());
        dto.setNormalizedText(normalizedText);
        dto.setDegraded(result.isDegraded());
        dto.setDegradeReason(result.getDegradeReason());
        dto.setCostMs(result.getCostMs());
        dto.setEndpoint(result.getEndpoint());
        dto.setAudioBytes(audioBytes);
        return dto;
    }
}
