package com.example.filter;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.phantomthief.scope.Scope;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.catalina.core.ApplicationPart;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * @author: badBoy
 * @create: 2024-11-18 16:20
 * @Description:
 */
public class FirstFilter extends OncePerRequestFilter {

    private static final String EXTERNAL_DATA_PARAM_NAME = "externalData";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("FirstFilter executed");
        /**
         * 工作台的附件提交都是先将附件提交到协同云，然后将协同云返回的附件id作为参数传到后端
         * 所以工作台的导出导入接口中不会有附件流的情况
         * 获得扩展数据赋值供应商数据信息
         */
        if (isMultipartFormDataType(request)) {
            JSONObject externalDataJson = JSON.parseObject(request.getParameter(EXTERNAL_DATA_PARAM_NAME));
            externalDataJson = Objects.isNull(externalDataJson) ? new JSONObject() : externalDataJson;
            String headerVal = request.getHeader("Content-Type");
            if (StringUtils.isNotBlank(headerVal)) {
                externalDataJson.put("Content-Type", headerVal);
            }
            request = getRequestWrapper(externalDataJson, request);
        } else if (StringUtils.contains(request.getContentType(), "json")) {
            String body = getBodyAsString(request);
            request = getRequestBodyWrapper(body, request);
        } else if (StringUtils.contains(request.getContentType(), "application/x-www-form-urlencoded")) {
            String body = getBodyAsString(request);
            request = getRequestBodyWrapper(body, request);
        } else {
            String body = getBodyAsString(request);
            request = getRequestBodyWrapper(body, request);
        }
        filterChain.doFilter(request, response);
    }

    private HttpServletRequest getRequestBodyWrapper(String bodyString, HttpServletRequest request) {
        JSONObject rawParamJson = JSON.parseObject(bodyString);
        rawParamJson = Objects.isNull(rawParamJson) ? new JSONObject() : rawParamJson;
        JSONObject externalDataJson = rawParamJson.getJSONObject(EXTERNAL_DATA_PARAM_NAME);
        externalDataJson = Objects.isNull(externalDataJson) ? new JSONObject() : externalDataJson;
        String headerVal = request.getHeader("Content-Type");
        externalDataJson.put("Content-Type", headerVal);
        rawParamJson.put(EXTERNAL_DATA_PARAM_NAME, externalDataJson);
        byte[] body = rawParamJson.toJSONString().getBytes();
        return new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                return getInputStreamWrapper(body);
            }
        };
    }

    private ServletInputStream getInputStreamWrapper(byte[] body) throws IOException {
        return new ServletInputStream() {
            private final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);

            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {

            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    private static String getBodyAsString(HttpServletRequest httpServletRequest) throws IOException {
        ContentCachingRequestWrapper requestWrapper;
        if (httpServletRequest instanceof ContentCachingRequestWrapper) {
            requestWrapper = (ContentCachingRequestWrapper) httpServletRequest;
        } else {
            requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        }
        String body = new String(requestWrapper.getContentAsByteArray());
        if (StringUtils.isBlank(body)) {
            String characterEncoding = httpServletRequest.getCharacterEncoding();
            if (StringUtils.isBlank(characterEncoding)) {
                characterEncoding = httpServletRequest.getHeader("characterEncoding");
            }
            body = IOUtils.toString(requestWrapper.getInputStream(), characterEncoding);
        }
        return body;
    }

    private Boolean isMultipartFormDataType(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            contentType = "";
        }
        int semicolon = contentType.indexOf(';');
        if (semicolon >= 0) {
            contentType = contentType.substring(0, semicolon).trim();
        } else {
            contentType = contentType.trim();
        }

        if ("multipart/form-data".equals(contentType)) {
            return true;
        }

        return false;
    }

    private HttpServletRequest getRequestWrapper(JSONObject externalDataJson, HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {

            @Override
            public String getQueryString() {
                Map<String, String> tenantGatewayMap = Maps.newHashMap();
                tenantGatewayMap.put("tenantId", "tenantId");
                StringBuffer stringBuffer = new StringBuffer(super.getQueryString());
                for (Map.Entry<String, String> entry : tenantGatewayMap.entrySet()) {
                    stringBuffer.append("&").append(entry.getKey()).append("=").append(entry.getValue());
                }
                return stringBuffer.toString();
            }

            @Override
            public String getParameter(String name) {
                if (StringUtils.equalsIgnoreCase(name, EXTERNAL_DATA_PARAM_NAME)) {
                    return externalDataJson.toJSONString();
                }
                return super.getParameter(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                Map<String, String[]> paramMap = super.getParameterMap();
                Map<String, String[]> filterMaps = Maps.newHashMap();
                if (MapUtil.isNotEmpty(paramMap)) {
                    for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                        String entryKey = entry.getKey();
                        if (StringUtils.equalsIgnoreCase(entryKey, EXTERNAL_DATA_PARAM_NAME)) {
                            filterMaps.put(entryKey, new String[]{externalDataJson.toJSONString()});
                        } else {
                            filterMaps.put(entryKey, entry.getValue());
                        }
                    }
                }
                return filterMaps;
            }

            @Override
            public String[] getParameterValues(String name) {
                if (StringUtils.equalsIgnoreCase(name, EXTERNAL_DATA_PARAM_NAME)) {
                    return new String[]{externalDataJson.toJSONString()};
                }
                return super.getParameterValues(name);
            }

            @Override
            public Collection<Part> getParts() throws IOException, ServletException {
                return FirstFilter.this.getParts(super.getParts(), externalDataJson);
            }

            @Override
            public Part getPart(String name) throws IOException, ServletException {
                Part part = super.getPart(name);
                if (StringUtils.equalsIgnoreCase(name, EXTERNAL_DATA_PARAM_NAME)) {
                    // 转换为字节数组
                    byte[] byteArray = externalDataJson.toJSONString().getBytes();
                    // 创建ByteArrayInputStream
                    InputStream inputStream = new ByteArrayInputStream(byteArray);
                    return new InputStreamPart(inputStream, (ApplicationPart) part);
                }
                return part;
            }
        };
    }

    private Collection<Part> getParts(Collection<Part> parts, JSONObject externalDataJson) {
        Collection<Part> filterParts = Lists.newArrayList();
        if (CollectionUtil.isNotEmpty(parts)) {
            for (Part part : parts) {
                String partName = part.getName();
                if (StringUtils.equalsIgnoreCase(partName, EXTERNAL_DATA_PARAM_NAME)) {
                    // 转换为字节数组
                    byte[] byteArray = externalDataJson.toJSONString().getBytes();
                    // 创建ByteArrayInputStream
                    InputStream inputStream = new ByteArrayInputStream(byteArray);
                    Part partNew = new InputStreamPart(inputStream, (ApplicationPart) part);
                    filterParts.add(partNew);
                } else {
                    filterParts.add(part);
                }
            }
        }
        return filterParts;
    }
}
