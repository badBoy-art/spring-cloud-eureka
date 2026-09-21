package com.example.asr;

import java.nio.file.Files;
import java.nio.file.Paths;

/** 测试用的 WAV 读取（复用主代码里的 WavUtils，避免各处重复硬编码头长度） */
final class AsrSmokeTestPcm {

    private AsrSmokeTestPcm() {
    }

    static byte[] read(String path) throws Exception {
        byte[] pcm = WavUtils.extractPcm(Files.readAllBytes(Paths.get(path)));
        if (pcm.length == 0) {
            throw new IllegalArgumentException("wav 无有效 PCM: " + path);
        }
        return pcm;
    }
}
