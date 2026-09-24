package com.example.rule.engine;

import com.example.rule.model.Order;
import org.kie.api.KieBase;
import org.kie.api.definition.KiePackage;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态规则引擎：一次编译 + 缓存 KieBase，规则变更时"先建新的、再原子换引用、最后 dispose 旧的"。
 * 绝不 per-request 编译（解析 + 字节码生成是几十到几百毫秒级）。
 *
 * 注意：每个应用实例都要自己刷新自己的 KieBase；多实例场景靠轮询版本号 / Redis pub-sub / MQ 广播触发。
 */
@Component
public class DynamicRuleEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRuleEngine.class);
    /** 单次会话最多点火次数：规则写错不收敛时不要挂死业务线程 */
    private static final int MAX_FIRES = 200;

    private volatile KieContainer container;
    private volatile KieBase kieBase;
    private volatile String activeVersion = "<未发布>";
    private volatile int ruleCount;
    private volatile int lastFireCount;

    /** 试编译：只编译不生效，用来在"发布"之前拦住脏规则。 */
    public void dryRun(Map<String, String> drlByName, Map<String, byte[]> xlsxByName) {
        KieContainer probe = KieModuleBuilder.build(drlByName, xlsxByName);
        probe.dispose();
    }

    /** 发布：编译成功才换引用，失败时旧版本继续对外服务。 */
    public synchronized Map<String, Object> publish(String version, Map<String, String> drlByName,
                                                    Map<String, byte[]> xlsxByName) {
        KieContainer next = KieModuleBuilder.build(drlByName, xlsxByName);
        KieBase nextBase = next.getKieBase();

        KieContainer previous = this.container;
        this.kieBase = nextBase;
        this.container = next;
        this.activeVersion = version;
        this.ruleCount = countRules(nextBase);
        if (previous != null) {
            previous.dispose();
        }
        LOGGER.info("规则已发布: version={} ruleCount={}", version, ruleCount);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeVersion", activeVersion);
        result.put("ruleCount", ruleCount);
        return result;
    }

    /** 用当前生效的规则跑一个事实对象（KieSession 非线程安全，每次新建 + dispose）。 */
    public synchronized RuleFireResult fire(Order order) {
        if (kieBase == null) {
            throw new IllegalStateException("规则尚未发布，请先配置并发布规则");
        }
        List<String> firedRules = new ArrayList<>();
        KieSession session = kieBase.newKieSession();
        try {
            session.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    firedRules.add(event.getMatch().getRule().getName());
                }
            });
            session.insert(order);
            int fired = session.fireAllRules(MAX_FIRES);
            this.lastFireCount = fired;
            if (fired >= MAX_FIRES) {
                LOGGER.warn("规则点火次数达到上限 {}，疑似规则不收敛（检查是否有 update() 无状态守卫的规则）", MAX_FIRES);
            }
            return new RuleFireResult(order, firedRules, fired);
        } finally {
            session.dispose();
        }
    }

    private int countRules(KieBase base) {
        int count = 0;
        for (KiePackage kiePackage : base.getKiePackages()) {
            count += kiePackage.getRules().size();
        }
        return count;
    }

    public String activeVersion() {
        return activeVersion;
    }

    public int ruleCount() {
        return ruleCount;
    }

    public int lastFireCount() {
        return lastFireCount;
    }
}
