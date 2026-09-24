package com.example.rule.engine;

import java.util.List;

/** 规则编译失败（页面要把这些行/列号原样回显给配置人，否则他们改不动）。 */
public class RuleCompileException extends RuntimeException {

    private final List<String> errors;

    public RuleCompileException(List<String> errors) {
        super("规则编译失败: " + errors);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
