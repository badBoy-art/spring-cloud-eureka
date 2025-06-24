package com.example.controller;

import com.alibaba.fastjson.JSON;
import com.example.vo.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * @author: badBoy
 * @create: 2025-06-24 16:00
 * @Description:
 */
@Component
public class Hello2Controller extends HelloController {

    @Override
    public ResponseEntity<User> uploadUser(HttpServletRequest request, User user) {
        System.out.println("hello2" + JSON.toJSONString(user));
        return super.uploadUser(request, user);
    }

}
