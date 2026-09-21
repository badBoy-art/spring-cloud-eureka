package com.example.asr;

import lombok.Data;

/**
 * base64 方式的语音识别入参（前端 H5 小程序常用）。
 *
 * @author badBoy
 */
@Data
public class AsrBase64Command {

    /** 音频 base64；16k 单声道 16bit PCM 或带头的 mp3/mp4 */
    private String audio;

    /** 音频格式：pcm / wav / mp3 / mp4，缺省按 pcm 处理 */
    private String format;
}
