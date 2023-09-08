package com.example.util;

import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * @author: badBoy
 * @create: 2023-09-08 14:17
 * @Description:
 */
@Service
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.applicationContext = applicationContext;
    }

    /**
     * 获取上下文
     */
    public static ApplicationContext getApplicationContext() {
        assertApplicationContext();
        return applicationContext;
    }

    /**
     * Bean是否存在
     *
     * @param name Bean名称
     */
    public static boolean containBean(String name) {
        return applicationContext.containsBean(name);
    }

    /**
     * 根据名称获取Bean
     *
     * @param beanName Bean名称
     * @return Bean
     */
    public static <T> T getBean(String beanName) {
        assertApplicationContext();
        return (T) applicationContext.getBean(beanName);
    }

    /**
     * 根据Bean名称和类型获取
     *
     * @param requiredType 类型
     */
    public static <T> T getBean(Class<T> requiredType) {
        assertApplicationContext();
        return applicationContext.getBean(requiredType);
    }

    public static <T> T getBean(String className, Class<T> requiredType) {
        assertApplicationContext();
        return applicationContext.getBean(className, requiredType);
    }

    /**
     * 获取同类型的所有bean
     *
     * @return k->beanName  v->bean
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> tClass) {
        assertApplicationContext();
        return applicationContext.getBeansOfType(tClass);
    }

    private static void assertApplicationContext() {
        if (SpringContextHolder.applicationContext == null) {
            throw new RuntimeException("applicaitonContext属性为null,请检查是否注入了SpringContextHolder!");
        }
    }

}
