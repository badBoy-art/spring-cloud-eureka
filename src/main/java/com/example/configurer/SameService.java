package com.example.configurer;

import cn.hutool.core.lang.Assert;
import com.example.service.SameTypeService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author: badBoy
 * @create: 2024-07-01 14:57
 * @Description:
 */
public class SameService implements InitializingBean {

    private SameTypeService typeServiceOne;

    private SameTypeService typeServiceTwo;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(typeServiceOne, "Harold is alive!");
        Assert.notNull(typeServiceTwo, "John is alive!");
    }

    /* Setter injection */
    @Autowired
    public void setPersonOne(SameTypeService typeServiceOne) {
        this.typeServiceOne = typeServiceOne;
        this.typeServiceOne.setFirstName("Harold");
        this.typeServiceOne.setSecondName("Finch");
    }

    @Autowired
    public void setPersonTwo(SameTypeService typeServiceTwo) {
        this.typeServiceTwo = typeServiceTwo;
        this.typeServiceTwo.setFirstName("John");
        this.typeServiceTwo.setSecondName("Reese");
    }
}
