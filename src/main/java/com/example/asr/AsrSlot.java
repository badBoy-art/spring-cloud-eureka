package com.example.asr;

import lombok.Getter;
import lombok.Setter;

import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;

/**
 * 连接池中的一个槽位：一条 WebSocket 长连接 + 它当前承载的那次请求。
 * <p>
 * 独占语义：同一条连接同一时刻只服务一个请求，用请求 ID（meta 帧的 wav_name）把响应关回来。
 * FunASR 的 C++ 服务端在 on_message 里按连接句柄维护各自的 data_map，收到新的 meta 帧即开新一段
 * 识别，所以同一条连接可以连续复用（压测时建议再确认一次）。
 *
 * @author badBoy
 */
@Getter
@Setter
class AsrSlot {

    /** 所属实例地址 */
    private final String endpoint;

    /** 长连接，onOpen 后可用 */
    private volatile WebSocket webSocket;

    /** 当前请求 ID（即 meta 帧的 wav_name） */
    private volatile String requestId;

    /** 当前请求等待响应的 future；响应里带 text 时 complete */
    private volatile CompletableFuture<String> pending;

    /** 最近一次活跃时间 */
    private volatile long lastActiveTime;

    /** false = 连接已损坏/已关闭，不能再用 */
    private volatile boolean usable = true;

    AsrSlot(String endpoint) {
        this.endpoint = endpoint;
    }

    void touch() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
