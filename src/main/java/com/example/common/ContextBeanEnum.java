package com.example.common;

import com.example.service.Speak;
import com.example.service.Speakable;
import com.example.util.SpringContextHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: badBoy
 * @create: 2023-09-08 14:12
 * @Description:
 */
@Getter
@AllArgsConstructor
public enum ContextBeanEnum {

    SPEAKABLE(1, ""),
    ;
    private int code;
    private String beanName;

    /**
     * pair left:beanName right:beanType <T extends Comparable<? super T>>
     *
     * @return
     */
    public <T extends Speak> Speak apply() {
        switch (this) {
            case SPEAKABLE:
                return SpringContextHolder.getBean(Speakable.class);
        }
        throw new AssertionError("Unknown type:" + this);
    }

}
