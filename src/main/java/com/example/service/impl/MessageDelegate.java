package com.example.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: badBoy
 * @create: 2023-06-02 17:44
 * @Description:
 */
@Slf4j
public class MessageDelegate {

    private AtomicInteger counter = new AtomicInteger();

    public void handleMessage(String message) {
        log.info("message ===== {}", message);
        counter.incrementAndGet();
    }

    public int getCount() {
        return counter.get();
    }
}
