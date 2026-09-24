package com.example.rule.store;

import com.example.rule.model.RuleAsset;
import com.example.rule.model.RuleDefinition;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 规则仓库（内存实现，零配置即可跑）。
 * 换 MySQL 只需要把这个类替换成 Dao：表结构见 ANALYSIS.md 第 4.1 节的 rule_asset / rule_definition。
 * 多实例部署时，发布动作要落到 DB 并由各实例轮询/订阅刷新自己的 KieBase。
 */
@Component
public class RuleRepository {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicLong idGenerator = new AtomicLong(1000);
    private final Map<Long, RuleDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Long> indexByKey = new ConcurrentHashMap<>();
    private final Map<String, RuleAsset> assets = new ConcurrentHashMap<>();

    /** 发布/覆盖：同 ruleKey 视为同一规则，版本 +1 并保留 id。 */
    public synchronized RuleDefinition publish(RuleDefinition candidate) {
        Long existsId = indexByKey.get(candidate.getRuleKey());
        if (existsId != null) {
            RuleDefinition old = definitions.get(existsId);
            candidate.setId(old.getId());
            candidate.setVersion(old.getVersion() + 1);
        } else {
            candidate.setId(idGenerator.incrementAndGet());
            candidate.setVersion(1);
            indexByKey.put(candidate.getRuleKey(), candidate.getId());
        }
        candidate.setUpdatedAt(LocalDateTime.now().format(TS));
        definitions.put(candidate.getId(), candidate);
        return candidate;
    }

    public List<RuleDefinition> all() {
        List<RuleDefinition> list = new ArrayList<>(definitions.values());
        list.sort(Comparator.comparingLong(RuleDefinition::getId));
        return list;
    }

    /** 只取生效的规则（status=1）交给引擎编译。 */
    public List<RuleDefinition> published() {
        List<RuleDefinition> list = new ArrayList<>();
        for (RuleDefinition definition : all()) {
            if (definition.getStatus() == 1) {
                list.add(definition);
            }
        }
        return list;
    }

    public RuleDefinition requireById(long id) {
        RuleDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("规则不存在: id=" + id);
        }
        return definition;
    }

    public RuleDefinition findByRuleName(String ruleName) {
        for (RuleDefinition definition : definitions.values()) {
            if (definition.getRuleName().equals(ruleName)) {
                return definition;
            }
        }
        return null;
    }

    public synchronized void changeStatus(long id, int status) {
        RuleDefinition definition = requireById(id);
        if (status != 0 && status != 1 && status != 2) {
            throw new IllegalArgumentException("status 只能是 0/1/2，当前: " + status);
        }
        definition.setStatus(status);
        definition.setUpdatedAt(LocalDateTime.now().format(TS));
    }

    public synchronized void remove(long id) {
        RuleDefinition removed = definitions.remove(id);
        if (removed != null) {
            indexByKey.remove(removed.getRuleKey());
        }
    }

    public RuleAsset asset(String assetKey) {
        RuleAsset asset = assets.get(assetKey);
        if (asset == null) {
            throw new IllegalArgumentException("决策表资产不存在: " + assetKey);
        }
        return asset;
    }

    public boolean hasAsset(String assetKey) {
        return assets.containsKey(assetKey);
    }

    public RuleAsset saveAsset(RuleAsset asset) {
        RuleAsset old = assets.get(asset.getAssetKey());
        asset.setVersion(old == null ? 1 : old.getVersion() + 1);
        asset.setUpdatedAt(LocalDateTime.now().format(TS));
        assets.put(asset.getAssetKey(), asset);
        return asset;
    }

    public List<RuleAsset> allAssets() {
        return new ArrayList<>(assets.values());
    }

    /** 发布用的全局版本标识：时间戳（生效规则数由引擎的 ruleCount 提供）。 */
    public String versionTag() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}
