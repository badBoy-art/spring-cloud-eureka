package com.example.configurer;

import com.example.filter.MyFilter;
import com.example.interceptor.MyInterceptor;
import com.example.resolver.IntegerCodeToEnumConverterFactory;
import com.example.resolver.IpMethodArgumentResolver;
import com.example.resolver.StringCodeToEnumConverterFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author: badBoy
 * @create: 2024-04-12 10:46
 * @Description:
 */
@Configuration
public class SpringMvcConfig {

    private static final int PROFILER_FILTER_FILTER_ORDER = 101;
    private static final int RPC_MONITOR_SEPARATE_URI_FILTER_ORDER = 102;

    @Bean
    WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            /**
             * 路径匹配配置
             */
            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer
                        // 是否开启后缀模式匹配，如 '/user' 是否匹配 '/user.*'，默认 true
                        .setUseSuffixPatternMatch(false)
                        // 是否开启后缀路径模式匹配，如 '/user' 是否匹配 '/user/'，默认 true
                        .setUseTrailingSlashMatch(true);
            }

            /**
             * 将对于静态资源的请求转发到 Servlet 容器的默认处理静态资源的 Servlet
             * 因为将 Spring 的拦截模式设置为 "/" 时会对静态资源进行拦截
             */
            @Override
            public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
                //关闭使用默认servlet处理静态资源
                //configurer.enable();
            }

            /**
             * 用于接口的登录验证-放行swagger接口与健康检查接口
             * @param registry
             */
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new MyInterceptor()).addPathPatterns("/**").excludePathPatterns("/error");
                //apiTokenInterceptor里面'已登陆用户'访问错误时（404\500...),重定向到/error，会调用 paramBundle().setUserId(userId)，导致NPE
            }

            /**
             * 用于解析@ip
             * @param resolvers
             */
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new IpMethodArgumentResolver());
            }

            @Override
            public void addFormatters(FormatterRegistry registry) {
                registry.addConverterFactory(new IntegerCodeToEnumConverterFactory());
                registry.addConverterFactory(new StringCodeToEnumConverterFactory());
            }
        };
    }

    @Bean
    @Primary
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        RequestContextFilter requestContextFilter = new RequestContextFilter();
        requestContextFilter.setThreadContextInheritable(true);
        registration.setFilter(requestContextFilter);
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<MyFilter> myFilter() {
        FilterRegistrationBean<MyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setOrder(Integer.MIN_VALUE);
        registrationBean.setFilter(new MyFilter());
        registrationBean.addUrlPatterns("/*"); // 指定过滤的URL
        return registrationBean;
    }

}


