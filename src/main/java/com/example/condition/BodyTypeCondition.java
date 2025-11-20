package com.example.condition;

/**
 * @author: badBoy
 * @create: 2025-11-19 23:51
 * @Description:
 */

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

/**
 * 2️⃣ 用于匹配请求体的 "type" 字段。
 * 当 expectedType 与 request.attribute("bodyType") 相等时返回自身，表示匹配成功；
 * 否则返回 null，表示此条件不满足。
 */
public class BodyTypeCondition implements RequestCondition<BodyTypeCondition> {
    private static final Logger log = LoggerFactory.getLogger(BodyTypeCondition.class);

    private final String expectedType;

    public BodyTypeCondition(String expectedType) {
        this.expectedType = expectedType;
    }

    /**
     * 组合两个同类条件，后面的（other）覆盖前面的（用于多个条件叠加时）。
     */
    @Override
    public BodyTypeCondition combine(BodyTypeCondition other) {
        return other != null ? other : this;
    }

    /**
     * 当请求满足此条件时返回自身；否则返回 null。
     */
    @Override
    public BodyTypeCondition getMatchingCondition(HttpServletRequest request) {
        String actual = (String) request.getAttribute("bodyType");
        log.debug(">>> BodyTypeCondition.getMatchingCondition: expected='{}', actual='{}'",
                expectedType, actual);
        if (actual == null) {
            // 如果请求体里根本没有 type 字段，则此条件不满足
            return null;
        }
        if (expectedType.equals(actual)) {
            return this;
        }
        return null;
    }

    /**
     * 用于多条件竞争时的排序。 这里简单的按字符串长度排序，长一点的更 “具体”。
     */
    @Override
    public int compareTo(BodyTypeCondition other, HttpServletRequest request) {
        return Integer.compare(this.expectedType.length(), other.expectedType.length());
    }

}
