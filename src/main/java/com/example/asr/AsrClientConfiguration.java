package com.example.asr;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ASR 客户端装配：连接池（启动即建满）+ 客户端 Bean + 独立线程池。
 *
 * @author badBoy
 */
@Slf4j
@Configuration
public class AsrClientConfiguration {

    /** 连接池在 Bean 初始化时就把长连接建满，流量建连时即均摊到各实例 */
    @Bean
    public AsrConnectionPool asrConnectionPool(AsrProperties props) {
        AsrConnectionPool pool = new AsrConnectionPool(props);
        if (props.isEnabled()) {
            pool.start();
        } else {
            log.warn("[asr] asr.enabled=false，连接池不启动，识别请求将直接降级");
        }
        return pool;
    }

    @Bean
    public AsrWebSocketClient asrWebSocketClient(AsrProperties props, AsrConnectionPool pool) {
        return new AsrWebSocketClient(props, pool);
    }

    /**
     * ASR 调用专用线程池：不要占用 Tomcat 工作线程池。
     * 队列满时用 CallerRunsPolicy 兜住（宁可慢一点，也不要静默丢弃请求）。
     */
    @Bean("asrExecutor")
    public ThreadPoolExecutor asrExecutor() {
        AtomicInteger seq = new AtomicInteger(0);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                8, 32, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(256),
                r -> {
                    Thread thread = new Thread(mdcAware(r), "asr-call-" + seq.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /** traceId 透传：把父线程 MDC 复制到工作线程，否则异步日志串不起来 */
    private static Runnable mdcAware(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            if (context != null) {
                MDC.setContextMap(new Hashtable<>(context));
            }
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
