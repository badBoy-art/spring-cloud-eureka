package com.example.controller;

import com.alibaba.fastjson.JSON;
import com.example.param.ImportParam;
import com.example.resolver.IP;
import com.example.vo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author: badBoy
 * @create: 2025-06-24 16:00
 * @Description:
 */
@Component
public class Hello2Controller extends HelloController {

    @RequestMapping(value = "hello/{param}/{param2}", method = RequestMethod.GET, params = {"param3=5"})
    @Override
    public ResponseEntity<String> hello(@NotNull HttpServletRequest request, @PathVariable("param") String param, @PathVariable("param2") String param2,
                                        @RequestParam(required = false) String param3, @IP String ip) {
        return super.hello(request, param, param2, param3 + "22222", ip);
    }

    @RequestMapping(value = "hello22", method = RequestMethod.GET)
    @Override
    public Long hello2(@NotNull HttpServletRequest request) {
        return super.hello2(request);
    }

    @RequestMapping(value = "uploadUser3", method = RequestMethod.POST)
    @Override
    public ResponseEntity<User> uploadUser(HttpServletRequest request, User user) {
        return super.uploadUser(request, user);
    }

    @RequestMapping(value = "uploadUser4", method = RequestMethod.POST)
    @Override
    public String uploadUser2(HttpServletRequest request, String param) {
        return super.uploadUser2(request, param);
    }

    @PostMapping("/uploadFile2")
    @Override
    public String handleFileUpload(MultipartFile file, String description) {
        return super.handleFileUpload(file, description);
    }

    @GetMapping("/download-zip2")
    @Override
    public void downloadZipFile(HttpServletResponse response) throws IOException {
        super.downloadZipFile(response);
    }

    @PostMapping("/externalData2")
    @Override
    public String externalData(ImportParam importParam) {
        return JSON.toJSONString(importParam);
    }
}
