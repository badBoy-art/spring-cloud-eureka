package com.example.rule.engine;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把一批规则资产（DRL 文本 + 决策表 xlsx 字节）编译成一个 KieContainer。
 * 没有 kmodule.xml，全部走 KieFileSystem 编程式装配。
 */
public final class KieModuleBuilder {

    private static final String ROOT = "src/main/resources/rules/";

    private KieModuleBuilder() {
    }

    public static KieContainer build(Map<String, String> drlByName, Map<String, byte[]> xlsxByName) {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();

        if (drlByName != null) {
            for (Map.Entry<String, String> entry : drlByName.entrySet()) {
                kfs.write(ROOT + entry.getKey() + ".drl", entry.getValue());
            }
        }
        if (xlsxByName != null) {
            for (Map.Entry<String, byte[]> entry : xlsxByName.entrySet()) {
                kfs.write(decisionTableResource(kieServices, entry.getKey(), entry.getValue()));
            }
        }

        KieBuilder builder = kieServices.newKieBuilder(kfs);
        builder.buildAll();
        if (builder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new RuleCompileException(toMessages(builder.getResults().getMessages()));
        }
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }

    private static Resource decisionTableResource(KieServices kieServices, String name, byte[] xlsx) {
        Resource resource = kieServices.getResources().newByteArrayResource(xlsx);
        resource.setSourcePath(ROOT + name + ".xlsx");
        resource.setResourceType(ResourceType.DTABLE);
        // 不显式指定 XLSX 会按老的 HSSF(.xls) 解析，直接报错
        DecisionTableConfiguration config = KnowledgeBuilderFactory.newDecisionTableConfiguration();
        config.setInputType(DecisionTableInputType.XLSX);
        resource.setConfiguration(config);
        return resource;
    }

    private static List<String> toMessages(List<Message> messages) {
        List<String> result = new ArrayList<>();
        for (Message message : messages) {
            result.add("第 " + message.getLine() + " 行第 " + message.getColumn() + " 列: " + message.getText());
        }
        return result;
    }
}
