package com.example.asr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音识别（FunASR runtime）客户端配置。
 * <p>
 * application.properties 示例：
 * <pre>
 * asr.enabled=true
 * asr.endpoints=ws://127.0.0.1:10095
 * asr.pool-size-per-endpoint=2
 * asr.request-timeout-ms=3000
 * asr.hotwords={"耐克":20}
 * </pre>
 *
 * @author badBoy
 */
@Data
@Component
@ConfigurationProperties(prefix = "asr")
public class AsrProperties {

    /** 总开关：置 false 时直接降级到键盘输入，不发起任何连接 */
    private boolean enabled = true;

    /** FunASR 实例地址（ws://ip:10095）。多实例并列，流量由客户端侧分配 */
    private List<String> endpoints = new ArrayList<>();

    /**
     * 每个实例建多少条长连接。建议等于该实例 run_server.sh 的 --decoder-thread-num
     * （16C32G 取 32；本地 docker 单实例取 2~4 足够）
     */
    private int poolSizePerEndpoint = 4;

    /** 单次识别端到端超时（含发送）。语音搜索是短句，线上一律 500ms；本地调试可放宽 */
    private int requestTimeoutMs = 3000;

    /** 单帧发送超时 */
    private int sendTimeoutMs = 500;

    /** 借不到空闲连接的等待上限。到了就降级，避免占用 Tomcat 线程 */
    private int borrowTimeoutMs = 100;

    /** 建连超时 */
    private int connectTimeoutMs = 3000;

    /** WebSocket 心跳间隔（秒），防止网关/防火墙切断长连接 */
    private int pingIntervalSeconds = 20;

    /** 音频分帧字节数：100ms @ 16k/16bit/单声道 = 3200 字节 */
    private int frameBytes = 3200;

    /** 热词 JSON，形如 {"耐克":20}；走首帧下发，多实例天然一致 */
    private String hotwords;

    /** 实例故障后的重连退避基数（秒），按失败次数翻倍，上限 60 秒 */
    private int reconnectBackoffSeconds = 5;

    /** 单次音频最短长度（字节），小于此值视为空录音直接降级。3200 ≈ 100ms */
    private int minAudioBytes = 3200;

    /** 单次音频最大长度（字节），超过则拒绝。~60 秒 16k 单声道 PCM ≈ 1.9MB */
    private int maxAudioBytes = 1920000;
}
