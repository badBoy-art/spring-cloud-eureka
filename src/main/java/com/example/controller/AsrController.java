package com.example.controller;

import com.example.asr.AsrAppService;
import com.example.asr.AsrBase64Command;
import com.example.asr.AsrVoiceDTO;
import com.example.response.BaseWebResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 语音识别接口（FunASR）。
 * <p>
 * Controller 只做入参日志 + 一行转发，业务在 AsrAppService。
 *
 * @author badBoy
 */
@Slf4j
@RestController
@RequestMapping("/asr")
public class AsrController {

    @Autowired
    private AsrAppService asrAppService;

    /**
     * 上传音频识别。音频要求 16k / 单声道 / 16bit PCM（wav 也可，服务端剥头），
     * 或带头的 mp3 / mp4。全程不落盘。
     */
    @PostMapping("/recognize")
    public BaseWebResponse<AsrVoiceDTO> recognize(@RequestPart("audio") MultipartFile audio) {
        log.info("语音识别(multipart), fileName={}, size={}",
                audio == null ? null : audio.getOriginalFilename(),
                audio == null ? 0 : audio.getSize());
        return BaseWebResponse.successWithData(asrAppService.recognize(audio));
    }

    /** base64 识别，H5/小程序拿不到 multipart 时的入口 */
    @PostMapping("/recognize-base64")
    public BaseWebResponse<AsrVoiceDTO> recognizeBase64(@RequestBody AsrBase64Command command) {
        log.info("语音识别(base64), format={}, base64Len={}", command.getFormat(),
                command.getAudio() == null ? 0 : command.getAudio().length());
        return BaseWebResponse.successWithData(
                asrAppService.recognizeBase64(command.getAudio(), command.getFormat()));
    }

    /** 连接池健康状态：idlePerEndpoint 全为 0 且 unhealthy 非空说明全部实例异常 */
    @GetMapping("/health")
    public BaseWebResponse<Map<String, Object>> health() {
        return BaseWebResponse.successWithData(asrAppService.health());
    }
}
