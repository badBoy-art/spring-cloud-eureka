package com.example.configurer;

import com.example.service.SameTypeService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Map;

/**
 * @author: badBoy
 * @create: 2024-07-01 14:51
 * @Description:
 */
public class TypeServiceFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<String, SameTypeService> map = beanFactory.getBeansOfType(SameTypeService.class);
        for (Map.Entry<String, SameTypeService> entry : map.entrySet()) {
            createInstances(beanFactory, entry.getKey(), entry.getValue());
        }
    }

    private void createInstances(ConfigurableListableBeanFactory beanFactory, String beanName, Object bean) {
        Qualifier qualifier = bean.getClass().getAnnotation(Qualifier.class);
        for (String name : extractNames(qualifier)) {
            Object newBean = beanFactory.getBean(beanName);
            beanFactory.registerSingleton(name.trim(), newBean);
        }
    }

    private String[] extractNames(Qualifier qualifier) {
        return qualifier.value().split(",");
    }

}
