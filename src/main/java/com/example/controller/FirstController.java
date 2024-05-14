package com.example.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author: badBoy
 * @create: 2024-05-13 19:37
 * @Description:
 */
public class FirstController extends BaseController {
    @RequestMapping(value = "someUrl", method = RequestMethod.GET)
    public String firstMethod() {
        return "first";
    }
}
