package com.example.configurer;

import org.springframework.context.SmartLifecycle;

/**
 * @author: badBoy
 * @create: 2023-10-25 09:25
 * @Description:
 * <a href="https://mp.weixin.qq.com/s/3X8agYSG_BPj_eAGHImY4A"/>
 */
public class MyLifeCycle2 implements SmartLifecycle {

    private volatile boolean running;

    @Override
    public void start() {
        running = true;
        System.out.println("------LifeCycle2 start-------");
    }

    @Override
    public void stop() {
        running = false;
        System.out.println("------LifeCycle2 stop------");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return SmartLifecycle.super.isAutoStartup();
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public int getPhase() {
        return 0;
    }

}
