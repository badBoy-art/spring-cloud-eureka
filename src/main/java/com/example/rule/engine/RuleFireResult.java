package com.example.rule.engine;

import com.example.rule.model.Order;

import java.util.List;

/** 一次规则执行的结果：事实对象 + 命中的规则名（页面据此展示"命中说明"）。 */
public record RuleFireResult(Order order, List<String> firedRules, int firedCount) {
}
