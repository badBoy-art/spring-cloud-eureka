package com.example.asr;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ASR 结果清洗 / 归一 / 别名纠错——识别文本进检索之前的最后一道工序。
 * <p>
 * 为什么必须有：语音搜索的成败不在 WER，而在「实体词」能否正确落到检索词上。
 * 例："给我找双三十七码的耐克鞋" → 需要变成 "37码 耐克 鞋" 才能命中商品；
 * 而"耐克"常被识别成"奈克/耐刻"，必须映射回平台规范词。
 * <p>
 * 生产建议：别名映射表与 ES 同义词表保持同一份数据源，别两处各维护一套。
 *
 * @author badBoy
 */
@Component
public class AsrTextNormalizer {

    /** 语气词/口头禅，留着会污染检索 */
    private static final List<String> FILLER_WORDS = Arrays.asList(
            "嗯", "呃", "啊", "哦", "额", "那个", "就是", "然后", "麻烦", "帮我", "给我", "我想", "我要");

    /** 同音/近音 → 平台规范词。示例数据，生产从配置或同义词表加载 */
    private static final Map<String, String> DEFAULT_ALIAS = new HashMap<>();

    static {
        DEFAULT_ALIAS.put("奈克", "耐克");
        DEFAULT_ALIAS.put("耐刻", "耐克");
        DEFAULT_ALIAS.put("阿迪", "阿迪达斯");
        DEFAULT_ALIAS.put("三十七码", "37码");
        DEFAULT_ALIAS.put("三十八码", "38码");
    }

    private final Map<String, String> alias = DEFAULT_ALIAS;

    /**
     * 清洗一段识别文本。返回空串表示"没有可用文本"，上层应回退到热门/默认结果，
     * 不要把空串丢给检索。
     */
    public String normalize(String asrText) {
        if (StrUtil.isBlank(asrText)) {
            return StrUtil.EMPTY;
        }
        String text = StrUtil.trim(asrText);
        text = removeFiller(text);
        text = replaceAlias(text);
        text = stripEdgePunctuation(text);
        // 单个无意义字符（如 "嗯"）直接判为无效
        return text.length() < 2 ? StrUtil.EMPTY : text;
    }

    private String removeFiller(String text) {
        String result = text;
        for (String filler : FILLER_WORDS) {
            result = result.replace(filler, " ");
        }
        return StrUtil.trim(StrUtil.replace(result, "  ", " "));
    }

    private String replaceAlias(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : alias.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String stripEdgePunctuation(String text) {
        return StrUtil.trim(StrUtil.strip(text, "，。！？、,.!?;；：:\"'“”‘’"));
    }
}
