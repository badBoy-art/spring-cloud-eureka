package com.example.configurer;

import org.springframework.context.Lifecycle;

/**
 * @author: badBoy
 * @create: 2023-10-25 09:04
 * @Description:
 */
public class MyLifecycle implements Lifecycle {

    private volatile boolean running;

    @Override
    public void start() {
        running = true;
        System.out.println("------Lifecycle start------");
    }

    @Override
    public void stop() {
        running = false;
        System.out.println("------Lifecycle stop------");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

}
