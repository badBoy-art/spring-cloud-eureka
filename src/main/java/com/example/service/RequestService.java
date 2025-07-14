package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * @author: badBoy
 * @create: 2025-07-08 19:55
 * @Description:
 */
@RequestScope
@Service
public class RequestService {

    public String getRequestScope() {
        return "requestScope" + this.hashCode();
    }

}
