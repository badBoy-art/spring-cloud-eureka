package com.example.service;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author: badBoy
 * @create: 2024-07-01 14:49
 * @Description:
 */
@Qualifier(value = "typeServiceOne, typeServiceTwo")
public class SameTypeService implements FactoryBean<Object> {

    private String firstName;

    private String secondName;

    public SameTypeService() {
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Object getObject() throws Exception {
        return new SameTypeService();
    }

    @Override
    public Class<?> getObjectType() {
        return SameTypeService.class;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }
}
