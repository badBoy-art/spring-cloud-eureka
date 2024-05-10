package com.example.service.monitor;

/**
 * @author xuedui.zhao
 * @create 2018-12-27
 */
public class MethodMonitor {

    private long start;
    private String method;

    public MethodMonitor(String method) {
        this.method = method;
        System.out.println("begin monitor..");
        this.start = System.currentTimeMillis();
    }

    public void log() {
        long elapsedTime = System.currentTimeMillis() - start;
        System.out.println("end monitor..");
        System.out.println("Method: " + method + ", execution time: " + elapsedTime + " milliseconds.");
    }
}
