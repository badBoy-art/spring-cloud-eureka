package com.example.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author: badBoy
 * @create: 2024-05-13 19:38
 * @Description:
 */
@RequestMapping("/bill")
public class SecondController extends BaseController {
    @RequestMapping(value = "/someUrl2", method = RequestMethod.GET)
    public String secondMethod() {
        return "second";
    }
}
