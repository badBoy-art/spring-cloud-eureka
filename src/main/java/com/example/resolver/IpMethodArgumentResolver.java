package com.example.resolver;

import cn.hutool.extra.servlet.ServletUtil;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author: badBoy
 * @create: 2023-08-25 16:57
 * @Description: 自定义参数解析器
 * <a href="https://developer.aliyun.com/article/1167822"></a>
 */
@Slf4j
public class IpMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String DEFAULT_VALUE = "body";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        IP ip = parameter.getParameterAnnotation(IP.class);
        return Objects.nonNull(ip);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return ServletUtil.getClientIP(request, null);
    }

}
