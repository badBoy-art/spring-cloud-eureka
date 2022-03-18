package com.example.serveice.monitor;

/**
 * @author xuedui.zhao
 * @create 2018-12-27
 */
public class MonitorSession {
    private static ThreadLocal<MethodMonitor> monitorThreadLocal = new ThreadLocal<>();

    public static void begin(String method) {
        MethodMonitor logger = new MethodMonitor(method);
        monitorThreadLocal.set(logger);
    }

    public static void end() {
        MethodMonitor logger = monitorThreadLocal.get();
        logger.log();
    }
}
