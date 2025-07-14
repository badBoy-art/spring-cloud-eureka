package com.example.service;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author: badBoy
 * @create: 2025-07-08 20:02
 * @Description:
 */
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Service
public class PrototypeService {

    public String getRequestScope() {
        return "sessionPrototype" + this.hashCode();
    }

}
