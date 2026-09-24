package com.example.rule.service;

import com.example.rule.catalog.RuleTypeCatalog;
import com.example.rule.dt.DecisionTableDrlExporter;
import com.example.rule.dt.DecisionTableGuard;
import com.example.rule.dt.DecisionTableXlsxWriter;
import com.example.rule.model.DecisionTableSpec;
import com.example.rule.model.RuleAsset;
import com.example.rule.store.RuleRepository;
import com.example.rule.store.RuleSourceLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 决策表资产的服务层：下载模板 / 上传试编译（含安全守门）/ 发布 / 停用。
 * 与 RuleAppService 共用同一条换版路径（RuleSourceLoader.publish），所以 DRL 规则和决策表能混编。
 */
@Service
public class DecisionTableAppService {

    public static final String DEFAULT_ASSET_KEY = "order_discount";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RuleRepository repository;
    private final RuleTypeCatalog catalog;
    private final DecisionTableXlsxWriter xlsxWriter;
    private final DecisionTableGuard guard;
    private final DecisionTableDrlExporter drlExporter;
    private final RuleSourceLoader sourceLoader;

    public DecisionTableAppService(RuleRepository repository, RuleTypeCatalog catalog,
                                   DecisionTableXlsxWriter xlsxWriter, DecisionTableGuard guard,
                                   DecisionTableDrlExporter drlExporter, RuleSourceLoader sourceLoader) {
        this.repository = repository;
        this.catalog = catalog;
        this.xlsxWriter = xlsxWriter;
        this.guard = guard;
        this.drlExporter = drlExporter;
        this.sourceLoader = sourceLoader;
    }

    /** 种一份默认为"未发布"的决策表：页面下载模板 → 填 → 上传 → 发布，正好演示闭环。 */
    @PostConstruct
    public void seedDefaultAsset() {
        if (repository.hasAsset(DEFAULT_ASSET_KEY)) {
            return;
        }
        DecisionTableSpec spec = catalog.defaultDecisionTableSpec();
        RuleAsset asset = new RuleAsset();
        asset.setAssetKey(DEFAULT_ASSET_KEY);
        asset.setAssetName("订单折扣决策表");
        asset.setSpec(spec);
        asset.setXlsx(xlsxWriter.write(spec, null));
        asset.setStatus(0);
        asset.setUpdatedAt(LocalDateTime.now().format(TS));
        repository.saveAsset(asset);
    }

    public List<Map<String, Object>> assets() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (RuleAsset asset : repository.allAssets()) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("assetKey", asset.getAssetKey());
            view.put("assetName", asset.getAssetName());
            view.put("status", asset.getStatus());
            view.put("version", asset.getVersion());
            view.put("updatedAt", asset.getUpdatedAt());
            view.put("conditionWhitelist", asset.getSpec().getConditionWhitelist());
            view.put("drlAvailable", drlExporter.available());
            list.add(view);
        }
        return list;
    }

    /** 页面"下载模板"：结构行由系统固化，业务方只填取值列。 */
    public byte[] template(String assetKey) {
        RuleAsset asset = repository.asset(assetKey);
        return xlsxWriter.write(asset.getSpec(), null);
    }

    /** 上传后先看会编译成什么 DRL（人工复核用），并做一次结构 + 取值安全校验。 */
    public Map<String, Object> previewDrl(String assetKey, byte[] xlsx) {
        RuleAsset asset = repository.asset(assetKey);
        guard.check(asset.getSpec(), xlsx);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("drl", drlExporter.toDrl(xlsx, assetKey + ".xlsx"));
        return result;
    }

    /**
     * 发布决策表：守门（结构 + 取值白名单）→ 与现有规则一起编译 → 成功才落库换版。
     * 注意顺序：编译在落库之前，所以校验不过的 xlsx 不会污染生效版本。
     */
    public Map<String, Object> publish(String assetKey, byte[] xlsx, String updatedBy) {
        RuleAsset asset = repository.asset(assetKey);
        guard.check(asset.getSpec(), xlsx);

        String drl = drlExporter.toDrl(xlsx, assetKey + ".xlsx");
        Map<String, Object> state = sourceLoader.publish(repository.versionTag(), null, Map.of(assetKey, xlsx));

        asset.setXlsx(xlsx);
        asset.setDrlContent(drl);
        asset.setStatus(1);
        asset.setUpdatedBy(updatedBy == null || updatedBy.isBlank() ? "admin" : updatedBy);
        RuleAsset saved = repository.saveAsset(asset);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetKey", saved.getAssetKey());
        result.put("version", saved.getVersion());
        result.put("status", saved.getStatus());
        result.put("updatedAt", saved.getUpdatedAt());
        result.put("drl", drl);
        result.putAll(state);
        return result;
    }

    /** 停用决策表：从生效集合里摘掉，DRL 规则不受影响。 */
    public Map<String, Object> disable(String assetKey) {
        RuleAsset asset = repository.asset(assetKey);
        asset.setStatus(2);
        repository.saveAsset(asset);
        return sourceLoader.publish(repository.versionTag(), null, null);
    }
}
