package com.example.asr;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FunASR 多实例连接池（客户端侧分配，方案 A）。
 * <p>
 * 职责：启动时对每个实例批量建满长连接 → 请求时按实例轮询借出空闲连接 → 用完归还；
 * 实例连续失败则摘除其全部连接并按退避重连；单实例过载时自动溢出到另一实例。
 * <p>
 * 为什么不用 L4 代理：WS 是长连接，代理只在建连时选后端；连接池自己选后端才能做
 * per-实例过载保护、权重灰度、秒级故障摘除。
 *
 * @author badBoy
 */
@Slf4j
public class AsrConnectionPool implements DisposableBean {

    /** 同一实例连续失败到达该次数即判定故障，摘除整实例 */
    private static final int UNHEALTHY_FAIL_THRESHOLD = 3;
    /** 重连退避上限（秒） */
    private static final long MAX_BACKOFF_SECONDS = 60L;

    private final AsrProperties props;
    private final HttpClient httpClient;
    /** endpoint -> 空闲连接队列 */
    private final Map<String, Deque<AsrSlot>> idleMap = new ConcurrentHashMap<>();
    /** endpoint -> 连续失败次数，onOpen 成功清零 */
    private final Map<String, AtomicInteger> failCountMap = new ConcurrentHashMap<>();
    /** 被摘除的实例，重连成功前不参与借出 */
    private final Set<String> unhealthy = ConcurrentHashMap.newKeySet();
    /** 轮询游标，保证多实例均匀分配 */
    private final AtomicInteger cursor = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;

    public AsrConnectionPool(AsrProperties props) {
        this.props = props;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "asr-pool-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                // WS 走 HTTP/1.1 升级，显式指定避免协议协商意外
                .version(HttpClient.Version.HTTP_1_1)
                .executor(Executors.newFixedThreadPool(4, r -> {
                    Thread thread = new Thread(r, "asr-ws-callback");
                    thread.setDaemon(true);
                    return thread;
                }))
                .build();
    }

    /** 启动即建满连接池——连接在启动时建立，流量才会在建连时就被均摊到各实例 */
    public void start() {
        for (String endpoint : props.getEndpoints()) {
            idleMap.putIfAbsent(endpoint, new ConcurrentLinkedDeque<>());
            failCountMap.putIfAbsent(endpoint, new AtomicInteger(0));
            connectBatch(endpoint, props.getPoolSizePerEndpoint());
        }
        // 心跳保活：长连接被网关/NAT 静默断开是 WS 方案最典型的线上问题。
        // java.net.http 没有 pingInterval 配置项，这里用 sendPing 自己做，
        // 顺带当成健康探针（ping 失败即判该连接损坏）。
        long pingSeconds = Math.max(props.getPingIntervalSeconds(), 5);
        scheduler.scheduleWithFixedDelay(this::heartbeat, pingSeconds, pingSeconds, TimeUnit.SECONDS);
        log.info("[asr] 连接池启动, endpoints={}, 每实例连接数={}, 心跳={}s", props.getEndpoints(),
                props.getPoolSizePerEndpoint(), pingSeconds);
    }

    /** 只对空闲连接发心跳：正在跑请求的连接不打扰 */
    private void heartbeat() {
        for (Map.Entry<String, Deque<AsrSlot>> entry : idleMap.entrySet()) {
            for (AsrSlot slot : entry.getValue()) {
                WebSocket webSocket = slot.getWebSocket();
                if (webSocket == null || !slot.isUsable()) {
                    continue;
                }
                try {
                    webSocket.sendPing(ByteBuffer.allocate(0)).whenComplete((ws, err) -> {
                        if (err != null) {
                            onSlotBroken(slot, "ping:" + err.getMessage());
                        }
                    });
                } catch (Exception e) {
                    onSlotBroken(slot, "ping:" + e.getMessage());
                }
            }
        }
    }

    /** 借一条空闲连接；取不到立即返回 null（上层降级），不做长时间阻塞 */
    public AsrSlot borrow() {
        List<String> endpoints = props.getEndpoints();
        if (endpoints == null || endpoints.isEmpty()) {
            return null;
        }
        long deadline = System.currentTimeMillis() + props.getBorrowTimeoutMs();
        do {
            for (int i = 0; i < endpoints.size(); i++) {
                String endpoint = endpoints.get(Math.floorMod(cursor.getAndIncrement(), endpoints.size()));
                if (unhealthy.contains(endpoint)) {
                    continue;
                }
                AsrSlot slot = pollIdle(endpoint);
                if (slot != null) {
                    return slot;
                }
            }
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            sleepQuietly(5L);
        } while (true);
        log.warn("[asr] 无空闲连接, unhealthy={}", unhealthy);
        return null;
    }

    /** 归还连接；已损坏的槽位不再入池，改为补建一条新连接 */
    public void release(AsrSlot slot) {
        if (slot == null) {
            return;
        }
        slot.setPending(null);
        slot.setRequestId(null);
        slot.touch();
        if (!slot.isUsable() || slot.getWebSocket() == null) {
            replenish(slot.getEndpoint());
            return;
        }
        Deque<AsrSlot> idle = idleMap.get(slot.getEndpoint());
        if (idle != null) {
            idle.offer(slot);
        }
    }

    /** 丢弃连接（超时/发送失败）。超时的那条连接状态不可信，必须重连，不能还池 */
    public void discard(AsrSlot slot, String reason) {
        if (slot == null) {
            return;
        }
        slot.setUsable(false);
        abortQuietly(slot);
        log.warn("[asr] 丢弃连接 endpoint={}, reason={}", slot.getEndpoint(), reason);
        replenish(slot.getEndpoint());
    }

    /** 服务端关闭或 IO 失败：槽位出池、唤醒等待中的请求、必要时摘除整实例 */
    void onSlotBroken(AsrSlot slot, String reason) {
        String endpoint = slot.getEndpoint();
        slot.setUsable(false);
        completePendingExceptionally(slot, reason);
        int fails = failCountMap.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
        if (fails >= UNHEALTHY_FAIL_THRESHOLD) {
            markUnhealthy(endpoint, reason);
        } else {
            replenish(endpoint);
        }
    }

    /** 建连成功：入池并清零失败计数 */
    void onSlotOpen(AsrSlot slot) {
        slot.touch();
        failCountMap.computeIfAbsent(slot.getEndpoint(), k -> new AtomicInteger(0)).set(0);
        Deque<AsrSlot> idle = idleMap.get(slot.getEndpoint());
        if (idle != null) {
            idle.offer(slot);
        } else {
            abortQuietly(slot);
        }
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
        for (Deque<AsrSlot> idle : idleMap.values()) {
            AsrSlot slot;
            while ((slot = idle.poll()) != null) {
                slot.setUsable(false);
                abortQuietly(slot);
            }
        }
        log.info("[asr] 连接池已关闭");
    }

    /** 池状态，供健康检查端点输出 */
    public Map<String, Object> status() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        Map<String, Integer> idle = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Deque<AsrSlot>> entry : idleMap.entrySet()) {
            idle.put(entry.getKey(), entry.getValue().size());
        }
        status.put("endpoints", props.getEndpoints());
        status.put("idlePerEndpoint", idle);
        status.put("unhealthy", unhealthy);
        status.put("enabled", props.isEnabled());
        return status;
    }

    // ------------------------------------------------------------------ 内部方法

    private AsrSlot pollIdle(String endpoint) {
        Deque<AsrSlot> idle = idleMap.get(endpoint);
        if (idle == null) {
            return null;
        }
        AsrSlot slot;
        while ((slot = idle.poll()) != null) {
            if (slot.isUsable() && slot.getWebSocket() != null) {
                return slot;
            }
            // 池里的僵尸连接（onClose 与还池竞态）就地清掉
            abortQuietly(slot);
            replenish(endpoint);
        }
        return null;
    }

    private void connectBatch(String endpoint, int count) {
        for (int i = 0; i < count; i++) {
            connectOne(endpoint);
        }
    }

    private void connectOne(String endpoint) {
        AsrSlot slot = new AsrSlot(endpoint);
        if (StrUtil.isBlank(endpoint) || !endpoint.startsWith("ws")) {
            log.error("[asr] 非法 endpoint={}（需要 ws://host:port）", endpoint);
            return;
        }
        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .buildAsync(URI.create(endpoint), new AsrListener(slot, this))
                // 建连失败（含 onError）统一走故障处理；onOpen 里已完成入池
                .exceptionally(e -> {
                    onSlotBroken(slot, "connect:" + e.getMessage());
                    return null;
                });
    }

    private void replenish(String endpoint) {
        if (unhealthy.contains(endpoint)) {
            return;
        }
        scheduler.execute(() -> {
            try {
                connectOne(endpoint);
            } catch (Exception e) {
                log.warn("[asr] 补建连接失败 endpoint={}", endpoint, e);
            }
        });
    }

    /** 实例连续失败 → 摘除全部连接，按退避重连；重连成功（onSlotOpen）后自动恢复 */
    private void markUnhealthy(String endpoint, String reason) {
        if (!unhealthy.add(endpoint)) {
            return;
        }
        log.error("[asr] 实例摘除 endpoint={}, reason={}", endpoint, reason);
        Deque<AsrSlot> idle = idleMap.get(endpoint);
        if (idle != null) {
            AsrSlot slot;
            while ((slot = idle.poll()) != null) {
                slot.setUsable(false);
                abortQuietly(slot);
            }
        }
        int fails = failCountMap.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).get();
        long backoff = Math.min(props.getReconnectBackoffSeconds() * (1L << Math.min(fails, 5)),
                MAX_BACKOFF_SECONDS);
        scheduler.schedule(() -> {
            unhealthy.remove(endpoint);
            failCountMap.get(endpoint).set(0);
            connectBatch(endpoint, props.getPoolSizePerEndpoint());
            log.info("[asr] 实例重连 endpoint={}, backoff={}s", endpoint, backoff);
        }, backoff, TimeUnit.SECONDS);
    }

    private void completePendingExceptionally(AsrSlot slot, String reason) {
        CompletableFuture<String> pending = slot.getPending();
        if (pending != null) {
            pending.completeExceptionally(new IllegalStateException("asr connection broken: " + reason));
        }
    }

    private void abortQuietly(AsrSlot slot) {
        try {
            if (slot.getWebSocket() != null) {
                slot.getWebSocket().abort();
            }
        } catch (Exception e) {
            log.debug("[asr] 关闭连接异常", e);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
