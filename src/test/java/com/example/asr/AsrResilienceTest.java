package com.example.asr;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * 韧性验收：单个 JVM 里持续调用，期间由外部把 ASR 服务停掉再拉起，
 * 观察「何时降级 / 何时自愈」，验证连接池的故障摘除与退避重连在真实服务上生效。
 * <p>
 * 用法: java com.example.asr.AsrResilienceTest ws://127.0.0.1:10095 /tmp/asr_test.wav [持续秒数=150] [间隔毫秒=3000]
 *
 * @author badBoy
 */
public class AsrResilienceTest {

    public static void main(String[] args) throws Exception {
        String endpoint = args.length > 0 ? args[0] : "ws://127.0.0.1:10095";
        String wavPath = args.length > 1 ? args[1] : "/tmp/asr_test.wav";
        int durationSec = args.length > 2 ? Integer.parseInt(args[2]) : 150;
        int intervalMs = args.length > 3 ? Integer.parseInt(args[3]) : 3000;

        AsrProperties props = new AsrProperties();
        props.setEndpoints(Collections.singletonList(endpoint));
        props.setPoolSizePerEndpoint(2);
        props.setRequestTimeoutMs(3000);
        props.setBorrowTimeoutMs(200);
        props.setConnectTimeoutMs(2000);
        props.setReconnectBackoffSeconds(5);
        props.setHotwords("{\"耐克\":20}");

        AsrConnectionPool pool = new AsrConnectionPool(props);
        pool.start();
        Thread.sleep(1500L);
        AsrWebSocketClient client = new AsrWebSocketClient(props, pool);
        byte[] pcm = AsrSmokeTestPcm.read(wavPath);

        long start = System.currentTimeMillis();
        int ok = 0;
        int degraded = 0;
        while (System.currentTimeMillis() - start < durationSec * 1000L) {
            long now = System.currentTimeMillis();
            AsrResult result = client.recognize(pcm, "pcm");
            String state = result.isDegraded()
                    ? "DEGRADED(" + result.getDegradeReason() + ")"
                    : "OK(" + result.getCostMs() + "ms)";
            System.out.printf("[t+%3ds] %-30s %s%n", (now - start) / 1000, state, result.getText());
            if (result.isDegraded()) {
                degraded++;
            } else {
                ok++;
            }
            Thread.sleep(intervalMs);
        }
        System.out.println("汇总: OK=" + ok + ", DEGRADED=" + degraded);
        System.out.println("pool.status=" + pool.status());
        pool.destroy();
    }
}
