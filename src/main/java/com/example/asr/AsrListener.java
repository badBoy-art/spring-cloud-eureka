package com.example.asr;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 单条长连接的 WebSocket 回调：建连入池、响应按 wav_name 关回请求、故障交给连接池处理。
 * <p>
 * 注意 java.net.http 的 WebSocket 是"按需拉取"模型：每收到一条消息都要再 request(1)，
 * 否则连接不会再推送消息。
 *
 * @author badBoy
 */
@Slf4j
class AsrListener implements WebSocket.Listener {

    private final AsrSlot slot;
    private final AsrConnectionPool pool;
    private final StringBuilder textBuffer = new StringBuilder();

    AsrListener(AsrSlot slot, AsrConnectionPool pool) {
        this.slot = slot;
        this.pool = pool;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        slot.touch();
        // 关键顺序：先把引用挂上槽位，再入池，避免借出时 webSocket 还是 null
        slot.setWebSocket(webSocket);
        pool.onSlotOpen(slot);
        log.info("[asr] 连接已建立, endpoint={}", slot.getEndpoint());
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        slot.touch();
        textBuffer.append(data);
        if (!last) {
            webSocket.request(1);
            return null;
        }
        String message = textBuffer.toString();
        textBuffer.setLength(0);
        handleMessage(message);
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        pool.onSlotBroken(slot, "closed:" + statusCode + ":" + reason);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        pool.onSlotBroken(slot, error.getClass().getSimpleName() + ":" + error.getMessage());
    }

    /**
     * 解析识别结果。完成判定以"收到带 text 的响应"为准，不要依赖 is_final
     * ——官方文档自相矛盾（示例写 true，参数说明写"offline 模式永远为 False"）。
     */
    private void handleMessage(String message) {
        JSONObject body;
        try {
            body = JSONUtil.parseObj(message);
        } catch (Exception e) {
            log.warn("[asr] 响应非 JSON, endpoint={}, body={}", slot.getEndpoint(),
                    StrUtil.maxLength(message, 200));
            return;
        }
        String requestId = slot.getRequestId();
        String wavName = body.getStr("wav_name");
        // 串号防护：只认本次请求的响应
        if (requestId != null && wavName != null && !requestId.equals(wavName)) {
            log.warn("[asr] 响应 requestId 不匹配, expect={}, actual={}", requestId, wavName);
            return;
        }
        String asrText = body.getStr("text");
        if (asrText == null) {
            // 还没有识别结果的中间帧（如仅回传连接确认），忽略
            return;
        }
        CompletableFuture<String> pending = slot.getPending();
        if (pending != null && pending.complete(asrText)) {
            log.info("[asr] 识别返回, endpoint={}, text={}", slot.getEndpoint(), asrText);
        }
    }
}
