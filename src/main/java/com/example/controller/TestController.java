package com.example.controller;

import com.example.netty.PushMsgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: badBoy
 * @create: 2023-03-06 19:36
 * @Description:
 */
@RestController()
@RequestMapping("push")
public class TestController {

    @Autowired
    private PushMsgService pushMsgService;

    @GetMapping("{uid}")
    public void pushOne(@PathVariable String uid) {
        pushMsgService.pushMsgToOne(uid, "Hello");
    }

}
