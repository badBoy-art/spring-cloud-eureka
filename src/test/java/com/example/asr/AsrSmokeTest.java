package com.example.asr;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * 语音识别客户端端到端冒烟测试（不需要 Spring 容器，可直接 main 跑）。
 * <p>
 * 用法：
 * <pre>
 * JAVA_HOME=&lt;JDK21&gt; java -cp target/classes:&lt;deps&gt; \
 *   com.example.asr.AsrSmokeTest ws://127.0.0.1:10095 /tmp/asr_test.wav 3
 * </pre>
 * 参数：endpoint、wav 路径（16k/单声道/16bit）、调用次数。
 *
 * @author badBoy
 */
public class AsrSmokeTest {

    public static void main(String[] args) throws Exception {
        String endpoint = args.length > 0 ? args[0] : "ws://127.0.0.1:10095";
        String wavPath = args.length > 1 ? args[1] : "/Users/admin/Downloads/asr_test.wav";
        int times = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        AsrProperties props = new AsrProperties();
        props.setEndpoints(Collections.singletonList(endpoint));
        props.setPoolSizePerEndpoint(2);
        props.setRequestTimeoutMs(5000);
        props.setBorrowTimeoutMs(500);
        props.setConnectTimeoutMs(3000);
        props.setHotwords("{\"耐克\":20,\"阿迪达斯\":20}");

        AsrConnectionPool pool = new AsrConnectionPool(props);
        pool.start();
        Thread.sleep(1200L); // 等建连；生产环境由启动预热保证，不靠 sleep

        AsrWebSocketClient client = new AsrWebSocketClient(props, pool);
        AsrTextNormalizer normalizer = new AsrTextNormalizer();
        byte[] pcm = readPcmFromWav(wavPath);
        System.out.println("[smoke] endpoint=" + endpoint + ", pcmBytes=" + pcm.length
                + ", poolSize=2");

        for (int i = 1; i <= times; i++) {
            long start = System.currentTimeMillis();
            AsrResult result = client.recognize(pcm, "pcm");
            System.out.println("[smoke] #" + i
                    + " degraded=" + result.isDegraded()
                    + " reason=" + result.getDegradeReason()
                    + " cost=" + (System.currentTimeMillis() - start) + "ms"
                    + " endpoint=" + result.getEndpoint()
                    + " text=" + result.getText());
        }

        System.out.println("[smoke] normalize(" + "嗯 那个 我要买 一双 奈克 三十七码 的 运动鞋。"
                + ") = " + normalizer.normalize("嗯 那个 我要买 一双 奈克 三十七码 的 运动鞋。"));
        System.out.println("[smoke] pool.status=" + pool.status());
        pool.destroy();
    }

    /** WAV 只发 data chunk 里的原始 PCM（头长度因录音端而异，必须解析，不能硬编码 44） */
    private static byte[] readPcmFromWav(String path) throws Exception {
        byte[] pcm = WavUtils.extractPcm(Files.readAllBytes(Paths.get(path)));
        if (pcm.length == 0) {
            throw new IllegalArgumentException("wav 文件无有效 PCM 数据: " + path);
        }
        return pcm;
    }
}
