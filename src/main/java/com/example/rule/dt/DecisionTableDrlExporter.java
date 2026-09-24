package com.example.rule.dt;

import org.drools.drl.extensions.DecisionTableProvider;
import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.springframework.stereotype.Component;

import java.util.ServiceLoader;

/**
 * 决策表 -> DRL 预览：页面拿它做"上传后先看会编译成什么规则"，人工复核再发布。
 * Drools 没有"xlsx 转 DRL"的公开工具类，用的是它的 SPI 实现（drools-drl-extensions 提供）。
 */
@Component
public class DecisionTableDrlExporter {

    private final DecisionTableProvider provider =
            ServiceLoader.load(DecisionTableProvider.class).findFirst().orElse(null);

    public String toDrl(byte[] xlsx, String sourcePath) {
        if (provider == null) {
            return null;
        }
        Resource resource = KieServices.Factory.get().getResources().newByteArrayResource(xlsx);
        resource.setSourcePath(sourcePath);
        resource.setResourceType(ResourceType.DTABLE);
        DecisionTableConfiguration config = KnowledgeBuilderFactory.newDecisionTableConfiguration();
        config.setInputType(DecisionTableInputType.XLSX);
        return provider.loadFromResource(resource, config);
    }

    /** SPI 是否可用（不可用时页面只显示"DRL 预览不可用"，不影响发布）。 */
    public boolean available() {
        return provider != null;
    }
}
