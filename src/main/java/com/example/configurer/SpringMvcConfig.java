package com.example.configurer;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.alibaba.fastjson.support.spring.JSONPResponseBodyAdvice;
import com.example.controller.FirstController;
import com.example.controller.SecondController;
import com.example.filter.FirstFilter;
import com.example.filter.MyFilter;
import com.example.filter.SecondFilter;
import com.example.interceptor.MyInterceptor;
import com.example.resolver.IntegerCodeToEnumConverterFactory;
import com.example.resolver.IpMethodArgumentResolver;
import com.example.resolver.StringCodeToEnumConverterFactory;
import com.example.service.SameTypeService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

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

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
                //自定义配置...
                //FastJsonConfig config = new FastJsonConfig();
                //config.set ...
                //converter.setFastJsonConfig(config);
                converters.add(0, converter);
            }

        };
    }

    /**
     * @return
     * @see ResponseBodyAdvice
     * 对返回结果进行统一封装
     */
    @Bean
    public JSONPResponseBodyAdvice jsonpResponseBodyAdvice() {
        return new JSONPResponseBodyAdvice();
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

    @Bean
    public FilterRegistrationBean<FirstFilter> firstFilter() {
        FilterRegistrationBean<FirstFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setOrder(Integer.MAX_VALUE + 12);
        registrationBean.setFilter(new FirstFilter());
        registrationBean.addUrlPatterns("/*"); // 指定过滤的URL
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<SecondFilter> secondFilter() {
        FilterRegistrationBean<SecondFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setOrder(Integer.MAX_VALUE + 10);
        registrationBean.setFilter(new SecondFilter());
        registrationBean.addUrlPatterns("/*"); // 指定过滤的URL
        return registrationBean;
    }

    @Bean
    public FirstController firstController() {
        return new FirstController();
    }

    @Bean
    public SecondController secondController() {
        return new SecondController();
    }


    @Bean
    public TypeServiceFactoryPostProcessor PersonFactoryPostProcessor() {
        return new TypeServiceFactoryPostProcessor();
    }

    @Bean
    public SameTypeService sameTypeService() {
        return new SameTypeService();
    }

    @Bean
    public SameService service() {
        return new SameService();
    }
}


