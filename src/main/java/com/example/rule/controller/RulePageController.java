package com.example.rule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * /rule/ui 这个短地址重定向到真正的页面。
 *
 * 不用 ViewController 的 "forward:/..." 视图名：本工程 @EnableWebMvc 会把 Boot 的
 * InternalResourceViewResolver 一起关掉，视图名前缀解析不了（会 500：
 * "Could not resolve view with name 'forward:...'"）。用 302 + location 最稳，
 * 而且 location 由 ServletUriComponentsBuilder 生成，自动带上 context-path。
 */
@Controller
public class RulePageController {

    @GetMapping("/rule/ui")
    public ResponseEntity<Void> page() {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/rule/ui/rule-admin.html")
                .build()
                .toUri();
        return ResponseEntity.status(302).location(location).build();
    }
}
