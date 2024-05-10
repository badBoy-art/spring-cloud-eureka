package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: badBoy
 * @create: 2023-12-12 16:55
 * @Description:
 */
@Component
public class WelcomeUtil {

    @Autowired
    public void setAddUserService(StaticService staticService) {
        WelcomeUtil.staticService = staticService;
    }

    private static StaticService staticService;

    public static String getS() {
        return staticService.getStr();
    }

}
