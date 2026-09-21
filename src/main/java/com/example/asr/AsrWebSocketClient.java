package com.example.asr;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * FunASR runtime 的 Java 客户端（非流式离线识别 + 连接池）。
 * <p>
 * 用法：{@code AsrResult r = asrClient.recognize(pcm16kBytes, "pcm");}
 * 入参必须是 16k / 单声道 / 16bit PCM，或带头的 mp3、mp4（webm/opus 需实测）。
 * <p>
 * 协议（每段音频三次交互）：
 * <pre>
 * 1) meta 帧  {"mode":"offline","wav_name":"&lt;uuid&gt;","wav_format":"pcm","is_speaking":true,
 *              "audio_fs":16000,"itn":true,"hotwords":"{\"耐克\":20}"}
 * 2) 音频分帧 bytes（100ms = 3200 字节，不要一次发全量）
 * 3) 结束帧   {"is_speaking":false}
 * 4) 响应     {"mode":"offline","wav_name":"&lt;uuid&gt;","text":"...","is_final":true,...}
 * </pre>
 * 官方两个 Java 组件都不能直接用于集成：FunasrWsClient 是 CLI 压测工具（无池、无超时），
 * java_http2ws_src 那个 Spring Boot demo 只发不收（TODO 未完成）+ 语法错误 + 每请求新建连接，
 * 所以这里自行实现「连接池 + 分帧 + 完成判定 + 超时重试降级」。
 *
 * @author badBoy
 */
@Slf4j
public class AsrWebSocketClient {

    /** 结束帧：告诉服务端一段音频发完了 */
    private static final String END_FRAME = "{\"is_speaking\":false}";
    /** 服务端要求的采样率 */
    private static final int AUDIO_FS = 16000;
    /** 一次识别最多尝试次数（第一次失败后换一条连接重试） */
    private static final int MAX_ATTEMPT = 2;

    private final AsrProperties props;
    private final AsrConnectionPool pool;

    public AsrWebSocketClient(AsrProperties props, AsrConnectionPool pool) {
        this.props = props;
        this.pool = pool;
    }

    /**
     * 识别一段音频。永不抛异常：失败/超时/无连接统一返回 degraded=true，由上层降级到键盘输入。
     *
     * @param audio 音频字节；pcm 为 16k 单声道 16bit 裸流，mp3/mp4 带文件头
     * @param wavFormat pcm / mp3 / mp4，对应 meta 帧的 wav_format
     */
    public AsrResult recognize(byte[] audio, String wavFormat) {
        long start = System.currentTimeMillis();
        String format = StrUtil.isBlank(wavFormat) ? "pcm" : wavFormat;
        if (!props.isEnabled()) {
            return AsrResult.degrade("asr_disabled", 0L);
        }
        if (audio == null || audio.length < props.getMinAudioBytes()) {
            return AsrResult.degrade("audio_too_short", elapsed(start));
        }
        if (audio.length > props.getMaxAudioBytes()) {
            return AsrResult.degrade("audio_too_long", elapsed(start));
        }

        String requestId = UUID.randomUUID().toString();
        String lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPT; attempt++) {
            String step = "attempt" + attempt;
            AsrSlot slot = pool.borrow();
            if (slot == null) {
                return AsrResult.degrade("no_idle_connection", elapsed(start));
            }
            try {
                String text = exchange(slot, requestId, audio, format);
                pool.release(slot);
                if (StrUtil.isBlank(text)) {
                    return AsrResult.degrade("empty_transcript", elapsed(start));
                }
                return AsrResult.ok(text, elapsed(start), slot.getEndpoint());
            } catch (TimeoutException e) {
                // 超时的连接状态不可信（可能还在跑上一段），丢弃并补建新连接，而不是还池
                pool.discard(slot, step + ":timeout");
                lastError = "timeout";
            } catch (Exception e) {
                pool.discard(slot, step + ":" + e.getClass().getSimpleName());
                lastError = e.getClass().getSimpleName() + ":" + e.getMessage();
                log.warn("[asr] 识别失败, attempt={}, endpoint={}", attempt, slot.getEndpoint(), e);
            }
        }
        log.error("[asr] 两次尝试均失败, 走降级, lastError={}", lastError);
        return AsrResult.degrade("asr_fail:" + lastError, elapsed(start));
    }

    /** 默认按 PCM 裸流识别 */
    public AsrResult recognize(byte[] pcm16k) {
        return recognize(pcm16k, "pcm");
    }

    // ------------------------------------------------------------------ 内部方法

    /** 一次完整收发：meta → 音频分帧 → 结束帧 → 等带 text 的响应 */
    private String exchange(AsrSlot slot, String requestId, byte[] audio, String wavFormat) throws Exception {
        WebSocket webSocket = slot.getWebSocket();
        if (webSocket == null) {
            throw new IllegalStateException("websocket not open");
        }
        CompletableFuture<String> pending = new CompletableFuture<>();
        slot.setPending(pending);
        slot.setRequestId(requestId);
        slot.touch();
        try {
            awaitSend(webSocket.sendText(buildMetaFrame(requestId, wavFormat), true));
            int frameBytes = props.getFrameBytes();
            for (int offset = 0; offset < audio.length; offset += frameBytes) {
                byte[] frame = Arrays.copyOfRange(audio, offset, Math.min(audio.length, offset + frameBytes));
                // 每一帧都是一条完整的二进制消息（与官方 Java-WebSocket 客户端行为一致）
                awaitSend(webSocket.sendBinary(ByteBuffer.wrap(frame), true));
            }
            awaitSend(webSocket.sendText(END_FRAME, true));
            return pending.get(props.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new IllegalStateException("connection broken: " + e.getCause().getMessage());
        } finally {
            slot.setPending(null);
            slot.setRequestId(null);
        }
    }

    /** 首帧 meta；热词通过该帧下发，多实例天然一致，不用改服务端 hotwords.txt */
    private String buildMetaFrame(String requestId, String wavFormat) {
        JSONObject meta = JSONUtil.createObj();
        meta.set("mode", "offline");
        meta.set("wav_name", requestId);
        meta.set("wav_format", wavFormat);
        meta.set("is_speaking", true);
        meta.set("audio_fs", AUDIO_FS);
        meta.set("itn", true);
        if (StrUtil.isNotBlank(props.getHotwords())) {
            meta.set("hotwords", props.getHotwords());
        }
        return JSONUtil.toJsonStr(meta);
    }

    private void awaitSend(CompletableFuture<WebSocket> future) throws Exception {
        future.get(props.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
