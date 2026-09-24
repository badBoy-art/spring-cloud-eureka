package com.example.rule.store;

import com.example.rule.engine.DynamicRuleEngine;
import com.example.rule.model.RuleAsset;
import com.example.rule.model.RuleDefinition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "盘库 + 发布"的公共入口：把仓库里所有生效的规则资产（DRL 文本 + 决策表 xlsx）收拢成一次编译。
 * 所有变更入口（发布规则、启停规则、发布决策表）都最终走到 publish()，保证只有一条换版路径。
 */
@Component
public class RuleSourceLoader {

    private final RuleRepository repository;
    private final DynamicRuleEngine engine;

    public RuleSourceLoader(RuleRepository repository, DynamicRuleEngine engine) {
        this.repository = repository;
        this.engine = engine;
    }

    public Map<String, String> collectDrl() {
        Map<String, String> map = new LinkedHashMap<>();
        for (RuleDefinition definition : repository.published()) {
            map.put(definition.getRuleName(), definition.getDrlContent());
        }
        return map;
    }

    public Map<String, byte[]> collectXlsx() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        for (RuleAsset asset : repository.allAssets()) {
            if (asset.getStatus() == 1 && asset.getXlsx() != null) {
                map.put(asset.getAssetKey(), asset.getXlsx());
            }
        }
        return map;
    }

    /** 编译并换版；编译失败会抛 RuleCompileException，调用方据此决定要不要落库。 */
    public Map<String, Object> publish(String versionTag, Map<String, String> extraDrl, Map<String, byte[]> extraXlsx) {
        Map<String, String> drl = collectDrl();
        if (extraDrl != null) {
            drl.putAll(extraDrl);
        }
        Map<String, byte[]> xlsx = collectXlsx();
        if (extraXlsx != null) {
            xlsx.putAll(extraXlsx);
        }
        return engine.publish(versionTag, drl, xlsx);
    }

    /** 试编译：不换版，用来在发布前拦住脏规则。 */
    public void dryRun(Map<String, String> extraDrl, Map<String, byte[]> extraXlsx) {
        Map<String, String> drl = collectDrl();
        if (extraDrl != null) {
            drl.putAll(extraDrl);
        }
        Map<String, byte[]> xlsx = collectXlsx();
        if (extraXlsx != null) {
            xlsx.putAll(extraXlsx);
        }
        engine.dryRun(drl, xlsx);
    }
}
