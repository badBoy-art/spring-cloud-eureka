package com.example.asr;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 语音识别业务编排：数据校验 → 调 ASR → 文本清洗 → 组装返回。
 * <p>
 * 约定：Controller 只代理，业务逻辑放这里；ASR 调用走独立的 asrExecutor，
 * 不占用 Tomcat 工作线程池（后者在网关流量突增时会被拖垮）。
 *
 * @author badBoy
 */
@Slf4j
@Service
public class AsrAppService {

    private static final String FORMAT_PCM = "pcm";
    private static final String FORMAT_MP3 = "mp3";
    private static final String FORMAT_MP4 = "mp4";

    private final AsrProperties props;
    private final AsrWebSocketClient asrClient;
    private final AsrTextNormalizer normalizer;
    private final AsrConnectionPool pool;
    private final ThreadPoolExecutor asrExecutor;

    public AsrAppService(AsrProperties props,
                         AsrWebSocketClient asrClient,
                         AsrTextNormalizer normalizer,
                         AsrConnectionPool pool,
                         @Qualifier("asrExecutor") ThreadPoolExecutor asrExecutor) {
        this.props = props;
        this.asrClient = asrClient;
        this.normalizer = normalizer;
        this.pool = pool;
        this.asrExecutor = asrExecutor;
    }

    /**
     * 上传文件识别（multipart，App/小程序/管理端通用）。
     * 音频只在内存里流转，不落盘、不入库、不打日志（合规要求）。
     */
    public AsrVoiceDTO recognize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return AsrVoiceDTO.of(AsrResult.degrade("empty_file", 0L), StrUtil.EMPTY, 0);
        }
        byte[] raw = readBytes(file);
        String format = detectFormat(file.getOriginalFilename(), raw);
        byte[] audio = normalizeAudio(raw, format);
        return doRecognize(audio, format, raw.length);
    }

    /** base64 识别（H5/小程序拿不到 multipart 时的入口） */
    public AsrVoiceDTO recognizeBase64(String base64, String format) {
        if (StrUtil.isBlank(base64)) {
            return AsrVoiceDTO.of(AsrResult.degrade("empty_audio", 0L), StrUtil.EMPTY, 0);
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(StrUtil.trim(base64));
        } catch (Exception e) {
            log.warn("[asr] base64 解码失败", e);
            return AsrVoiceDTO.of(AsrResult.degrade("base64_decode_fail", 0L), StrUtil.EMPTY, 0);
        }
        String actualFormat = StrUtil.isBlank(format) ? detectFormat(null, raw) : format.toLowerCase();
        byte[] audio = normalizeAudio(raw, actualFormat);
        return doRecognize(audio, actualFormat, raw.length);
    }

    /** 池状态，供 /asr/health 输出 */
    public Map<String, Object> health() {
        return pool.status();
    }

    // ------------------------------------------------------------------ 内部方法

    /** 调 ASR：独立线程池 + 硬超时兜底，保证 servlet 线程绝不会被挂住 */
    private AsrVoiceDTO doRecognize(byte[] audio, String format, int rawBytes) {
        long start = System.currentTimeMillis();
        AsrResult result;
        try {
            Future<AsrResult> future = asrExecutor.submit(() -> asrClient.recognize(audio, format));
            result = future.get(props.getRequestTimeoutMs() + 500L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            result = AsrResult.degrade("executor_timeout", System.currentTimeMillis() - start);
            log.error("[asr] 调用超时, format={}, bytes={}", format, audio.length);
        } catch (Exception e) {
            result = AsrResult.degrade("executor_error:" + e.getClass().getSimpleName(),
                    System.currentTimeMillis() - start);
            log.error("[asr] 调用异常, format={}, bytes={}", format, audio.length, e);
        }
        String normalized = result.isDegraded() ? StrUtil.EMPTY : normalizer.normalize(result.getText());
        // 识别文本为空或太短 → 交给上层走热门/默认结果，不要把空串丢给检索
        return AsrVoiceDTO.of(result, normalized, rawBytes);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new IllegalStateException("读取上传音频失败", e);
        }
    }

    /** 按文件名 + 文件头判格式：FunASR 接受 pcm 裸流与带头的 mp3/mp4 */
    private String detectFormat(String fileName, byte[] raw) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".mp3")) {
            return FORMAT_MP3;
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".m4a") || lower.endsWith(".aac")) {
            return FORMAT_MP4;
        }
        if (WavUtils.isWav(raw)) {
            return FORMAT_PCM;
        }
        return FORMAT_PCM;
    }

    /**
     * wav 只发 data chunk 里的原始 PCM（头部长度因录音端而异：macOS afconvert 是 4096 字节，
     * 很多安卓录音是 44 字节，硬编码必然出错）；pcm 裸流原样发。
     */
    private byte[] normalizeAudio(byte[] raw, String format) {
        if (!FORMAT_PCM.equals(format)) {
            return raw;
        }
        return WavUtils.extractPcm(raw);
    }
}
