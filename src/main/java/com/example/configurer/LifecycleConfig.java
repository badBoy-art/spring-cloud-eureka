package com.example.configurer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: badBoy
 * @create: 2023-10-25 09:13
 * @Description:
 */
@Configuration
public class LifecycleConfig {

    @Bean
    public MyLifecycle myLifecycle() {
        return new MyLifecycle();
    }

    @Bean
    public MyLifeCycle2 myLifeCycle2() {
        return new MyLifeCycle2();
    }

}
