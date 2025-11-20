package com.example.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 增强版请求体类型提取过滤器
 * 解决请求体为空的问题
 */
//@Component
public class BodyTypeExtractingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BodyTypeExtractingFilter.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    // 支持的媒体类型
    private static final MediaType[] SUPPORTED_MEDIA_TYPES = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8, MediaType.valueOf("application/json;charset=UTF-8"), MediaType.valueOf(
            "application/hal+json")};

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        // 记录详细请求信息
        log.debug("=== 过滤器开始 ===");
        log.debug("请求方法: {}", request.getMethod());
        log.debug("请求URI: {}", request.getRequestURI());
        log.debug("Content-Type: {}", request.getContentType());
        log.debug("Content-Length: {}", request.getContentLength());
        log.debug("Query String: {}", request.getQueryString());

        // 检查是否需要处理
        if (!shouldProcessRequest(request)) {
            log.debug("跳过请求处理");
            chain.doFilter(request, response);
            return;
        }

        try {
            // 创建包装器并尝试读取请求体
            ContentCachingRequestWrapper cachingWrapper = new ContentCachingRequestWrapper(request);
            String type = extractRequestBodyType(cachingWrapper);
            request.setAttribute("bodyType", type);

            log.info(">>> Filtering request: {} {}, extracted type='{}'", request.getMethod(), request.getRequestURI(), type);

            // 创建响应包装器

            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

            try {
                chain.doFilter(cachingWrapper, responseWrapper);
            } finally {
                responseWrapper.copyBodyToResponse();
            }

        } catch (Exception e) {
            log.error("处理请求时出错", e);
            // 即使出错也要继续处理链
            chain.doFilter(request, response);
        }
    }

    /**
     * 判断是否应该处理此请求
     */
    private boolean shouldProcessRequest(HttpServletRequest request) {
        // 只处理有请求体的方法
        if (!hasRequestBody(request.getMethod())) {
            log.debug("方法 {} 没有请求体", request.getMethod());
            return false;
        }

        // 检查 Content-Type
        String contentType = request.getContentType();
        if (!isSupportedMediaType(contentType)) {
            log.debug("不支持的 Content-Type: {}", contentType);
            return false;
        }

        // 检查是否有实际请求体
        if (request.getContentLength() <= 0) {
            log.debug("请求体长度为 0");
            return false;
        }

        return true;
    }

    /**
     * 判断是否有请求体
     */
    private boolean hasRequestBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    /**
     * 检查媒体类型是否支持
     */
    private boolean isSupportedMediaType(String contentType) {
        if (contentType == null) {
            return false;
        }

        try {
            MediaType requestType = MediaType.parseMediaType(contentType);
            return Arrays.stream(SUPPORTED_MEDIA_TYPES).anyMatch(type -> requestType.isCompatibleWith(type));
        } catch (Exception e) {
            log.warn("解析媒体类型失败: {}", contentType, e);
            return false;
        }
    }

    /**
     * 提取请求体类型 - 多种方式尝试
     */
    private String extractRequestBodyType(ContentCachingRequestWrapper cachingWrapper) throws IOException {
        // 方法1: 使用 ContentCachingRequestWrapper

        byte[] body = cachingWrapper.getContentAsByteArray();

        if (body != null && body.length > 0) {
            log.debug("方法1成功: ContentCachingRequestWrapper 获取到 {} 字节", body.length);
            String type = parseJsonType(body);
            if (type != null) {
                return type;
            }
        }

        // 方法2: 直接读取 InputStream
        try {
            body = readInputStream(cachingWrapper.getInputStream());
            if (body != null && body.length > 0) {
                log.debug("方法2成功: 直接读取 InputStream 获取到 {} 字节", body.length);
                String type = parseJsonType(body);
                if (type != null) {
                    return type;
                }
            }
        } catch (Exception e) {
            log.warn("方法2失败: 直接读取 InputStream 时出错", e);
        }

        // 方法3: 从查询参数获取
        String queryType = cachingWrapper.getParameter("type");
        if (queryType != null && !queryType.isEmpty()) {
            log.debug("方法3成功: 从查询参数获取 type = {}", queryType);
            return queryType;
        }

        log.warn("所有方法都未能获取到请求体数据");
        return null;
    }

    /**
     * 从 InputStream 读取数据
     */
    private byte[] readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }

        // 使用缓冲读取
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        return outputStream.toByteArray();
    }

    /**
     * 解析 JSON 中的 type 字段
     */
    private String parseJsonType(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(body); JsonParser parser = JSON_FACTORY.createParser(bis)) {

            // 确保这是 JSON 对象
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                log.warn("JSON 不是对象格式");
                return null;
            }

            // 遍历对象字段
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.getCurrentName();
                parser.nextToken(); // 移动到值

                if ("type".equals(fieldName)) {
                    String typeValue = parser.getText();
                    log.debug("成功解析 type 字段: {}", typeValue);
                    return typeValue;
                } else {
                    // 跳过当前字段的子结构
                    parser.skipChildren();
                }
            }

            log.debug("未找到 type 字段");
            return null;

        } catch (Exception e) {
            log.error("解析 JSON 时出错", e);
            log.debug("原始数据: {}", new String(body, StandardCharsets.UTF_8));
            return null;
        }
    }

    /**
     * 工具方法：检查请求体是否为空
     */
    public static boolean isRequestBodyEmpty(HttpServletRequest request) {
        String type = (String) request.getAttribute("bodyType");
        return type == null;
    }

    /**
     * 工具方法：获取提取的类型
     */
    public static String getExtractedType(HttpServletRequest request) {
        return (String) request.getAttribute("bodyType");
    }
}
