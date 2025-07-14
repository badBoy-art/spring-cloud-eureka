package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * @author: badBoy
 * @create: 2025-07-08 19:57
 * @Description:
 */
@SessionScope
@Service
public class SessionService {

    public String getRequestScope() {
        return "sessionScope" + this.hashCode();
    }

}
