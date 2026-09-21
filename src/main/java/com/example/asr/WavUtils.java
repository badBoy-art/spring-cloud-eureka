package com.example.asr;

/**
 * WAV 头解析工具。
 * <p>
 * 为什么不能硬编码 44 字节：WAV 头长度取决于写入端，实测 macOS `say/afconvert` 生成的
 * WAV 是 4096 字节头（含 LIST/fact 等 chunk），硬编码 44 会把 4052 字节头部当音频发给 ASR。
 * 正确做法是遍历 chunk 找到 "data" 的偏移。
 *
 * @author badBoy
 */
public final class WavUtils {

    private WavUtils() {
    }

    /** 是否 WAV（RIFF....WAVE） */
    public static boolean isWav(byte[] data) {
        return data != null && data.length > 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';
    }

    /**
     * 返回 PCM 数据起始偏移；非 WAV 返回 0（调用方按裸 PCM 处理）。
     */
    public static int findDataOffset(byte[] data) {
        if (!isWav(data)) {
            return 0;
        }
        int pos = 12;
        while (pos + 8 <= data.length) {
            String chunkId = new String(data, pos, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int size = (data[pos + 4] & 0xFF)
                    | ((data[pos + 5] & 0xFF) << 8)
                    | ((data[pos + 6] & 0xFF) << 16)
                    | ((data[pos + 7] & 0xFF) << 24);
            if ("data".equals(chunkId)) {
                return pos + 8;
            }
            if (size < 0) {
                break;
            }
            pos += 8 + size + (size % 2);
        }
        return 0;
    }

    /**
     * 取出 WAV 的原始 PCM；非 WAV 原样返回。
     */
    public static byte[] extractPcm(byte[] data) {
        if (!isWav(data)) {
            return data;
        }
        int offset = findDataOffset(data);
        if (offset <= 0 || offset >= data.length) {
            return new byte[0];
        }
        byte[] pcm = new byte[data.length - offset];
        System.arraycopy(data, offset, pcm, 0, pcm.length);
        return pcm;
    }
}
