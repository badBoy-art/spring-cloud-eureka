package com.example.configurer;

import com.example.resolver.IpMethodArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: badBoy
 * @create: 2023-10-26 17:18
 * @Description:
 */
@Configuration
public class MethodArgumentResolverConfig {

    @Bean
    public WebMvcConfigurer getWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new IpMethodArgumentResolver());
            }
        };
    }

}
