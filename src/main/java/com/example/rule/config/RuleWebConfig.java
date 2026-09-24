package com.example.rule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 让规则管理页面能被访问到。
 *
 * 两个坑都得绕开：
 *  1) 本工程用了 @EnableWebMvc + 显式关闭 DefaultServletHandler，Spring Boot 的静态资源自动配置
 *     会整体退让，classpath:/static/ 下的页面直接 404，必须自己注册资源处理器；
 *  2) 工程里已有 @GetMapping("/{sex}") 这种单段通配映射（ResponsiveController），
 *     放在根路径下的 /rule-admin.html 会被它先匹配走，于是必须放在两段路径 /rule/ui/** 下。
 *
 * 访问：http://localhost:9008/eurekaclient/rule/ui
 */
@Configuration
public class RuleWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/rule/ui/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }
}
