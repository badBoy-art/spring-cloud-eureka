package com.example.controller;

import com.example.rule.engine.RuleCompileException;
import com.example.rule.service.DecisionTableAppService;
import com.example.rule.service.RuleAppService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则管理端接口（严格做转发，业务都在 AppService 里）。
 * 页面：http://localhost:9008/eurekaclient/rule-admin.html
 */
@RestController
@RequestMapping("/rule")
public class RuleAdminController {

    private final RuleAppService ruleAppService;
    private final DecisionTableAppService decisionTableAppService;

    public RuleAdminController(RuleAppService ruleAppService, DecisionTableAppService decisionTableAppService) {
        this.ruleAppService = ruleAppService;
        this.decisionTableAppService = decisionTableAppService;
    }

    // ===== 规则类型 + 页面表单 =====

    @GetMapping("/type/meta")
    public Object typeMeta() {
        return ruleAppService.types();
    }

    // ===== 参数模板化规则 =====

    @PostMapping("/rule/preview")
    public Object preview(@RequestBody RuleRequest request) {
        return ruleAppService.preview(request.ruleType(), request.params());
    }

    @PostMapping("/rule/validate")
    public Object validate(@RequestBody RuleRequest request) {
        return ruleAppService.validate(request.ruleType(), request.params());
    }

    @PostMapping("/rule/publish")
    public Object publish(@RequestBody RuleRequest request) {
        return ruleAppService.publish(request.ruleType(), request.params(), request.ruleKey(), request.updatedBy());
    }

    @GetMapping("/rule/list")
    public Object list() {
        return ruleAppService.definitions();
    }

    @PostMapping("/rule/status")
    public Object changeStatus(@RequestBody StatusRequest request) {
        return ruleAppService.changeStatus(request.id(), request.status());
    }

    @PostMapping("/rule/delete")
    public Object delete(@RequestBody StatusRequest request) {
        return ruleAppService.remove(request.id());
    }

    @PostMapping("/refresh")
    public Object refresh() {
        return ruleAppService.refresh();
    }

    @GetMapping("/state")
    public Object state() {
        return ruleAppService.state();
    }

    @PostMapping("/run")
    public Object run(@RequestBody RunRequest request) {
        return ruleAppService.run(request.customerLevel(), request.region(), request.amount(), request.itemCount());
    }

    // ===== Excel 决策表 =====

    @GetMapping("/dt/assets")
    public Object assets() {
        return decisionTableAppService.assets();
    }

    @GetMapping("/dt/template")
    public ResponseEntity<byte[]> template(@RequestParam(defaultValue = DecisionTableAppService.DEFAULT_ASSET_KEY) String assetKey) {
        byte[] xlsx = decisionTableAppService.template(assetKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + assetKey + "-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @PostMapping("/dt/preview")
    public Object previewDecisionTable(@RequestParam(defaultValue = DecisionTableAppService.DEFAULT_ASSET_KEY) String assetKey,
                                       @RequestParam("file") MultipartFile file) throws IOException {
        return decisionTableAppService.previewDrl(assetKey, file.getBytes());
    }

    @PostMapping("/dt/publish")
    public Object publishDecisionTable(@RequestParam(defaultValue = DecisionTableAppService.DEFAULT_ASSET_KEY) String assetKey,
                                       @RequestParam(required = false) String updatedBy,
                                       @RequestParam("file") MultipartFile file) throws IOException {
        return decisionTableAppService.publish(assetKey, file.getBytes(), updatedBy);
    }

    @PostMapping("/dt/disable")
    public Object disableDecisionTable(@RequestParam(defaultValue = DecisionTableAppService.DEFAULT_ASSET_KEY) String assetKey) {
        return decisionTableAppService.disable(assetKey);
    }

    // ===== 统一错误出口：校验不过 / 编译不过都把原因讲清楚，页面直接显示 =====

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Object> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(error(e.getMessage(), null));
    }

    @ExceptionHandler(RuleCompileException.class)
    public ResponseEntity<Object> handleCompileError(RuleCompileException e) {
        return ResponseEntity.badRequest().body(error("规则编译失败，未生效", e.getErrors()));
    }

    private Map<String, Object> error(String message, Object details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        if (details != null) {
            body.put("details", details);
        }
        return body;
    }

    public record RuleRequest(String ruleType, String ruleKey, Map<String, String> params, String updatedBy) {
    }

    public record StatusRequest(long id, int status) {
    }

    public record RunRequest(String customerLevel, String region, Double amount, Integer itemCount) {
    }
}
