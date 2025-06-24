package com.example.filter;

import jakarta.servlet.http.Part;
import org.apache.catalina.core.ApplicationPart;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author: badBoy
 * @create: 2024-11-21 13:11
 * @Description:
 */
public class InputStreamPart implements Part {

    private InputStream inputStream;
    private ApplicationPart applicationPart;

    public InputStreamPart(InputStream inputStream, ApplicationPart applicationPart) {
        this.applicationPart = applicationPart;
        this.inputStream = inputStream;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public String getContentType() {
        return applicationPart.getContentType();
    }

    @Override
    public String getName() {
        return applicationPart.getName();
    }

    @Override
    public String getSubmittedFileName() {
        return applicationPart.getSubmittedFileName();
    }

    @Override
    public long getSize() {
        return applicationPart.getSize();
    }

    @Override
    public void write(String s) throws IOException {
        applicationPart.write(s);
    }

    @Override
    public void delete() throws IOException {
        applicationPart.delete();
    }

    @Override
    public String getHeader(String s) {
        return applicationPart.getHeader(s);
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return applicationPart.getHeaders(s);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return applicationPart.getHeaderNames();
    }
}
