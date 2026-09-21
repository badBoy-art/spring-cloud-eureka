package com.example.registrar;

import com.example.condition.BodyTypeCondition;
import com.example.configurer.BodyTypeMapping;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 4️⃣ Registrar：在容器初始化结束后把所有标记了 @BodyTypeMapping 的方法
 * 手工注册到 RequestMappingHandlerMapping 中，并带上我们自定义的 BodyTypeCondition。
 */
@Component
public class BodyTypeMappingRegistrar implements BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    // 保存待注册的映射信息
    private final List<MappingRegistration> pendingRegistrations = new CopyOnWriteArrayList<>();

    // 已注册的映射缓存
    private final ConcurrentHashMap<String, Boolean> registeredMappings = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 只处理控制器类
        if (isController(bean.getClass())) {
            registerMappingsFromBean(bean);
        }
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        WebApplicationContext context = (WebApplicationContext) event.getApplicationContext();

        // 检查是否为Web环境
        ServletContext servletContext = context.getServletContext();
        if (servletContext == null) {
            System.out.println("ℹ️ 非Web环境，跳过自定义映射注册");
            return;
        }

        // 获取HandlerMapping（可能在此时才可用）
        RequestMappingHandlerMapping handlerMapping = context.getBean("requestMappingHandlerMapping",
                RequestMappingHandlerMapping.class);

        if (handlerMapping == null) {
            System.out.println("⚠️ HandlerMapping bean 不可用，延迟注册");
            return;
        }

        // 执行所有待注册的映射
        registerPendingMappings(handlerMapping);
    }

    /**
     * 从控制器Bean中注册映射
     */
    private void registerMappingsFromBean(Object bean) {
        Class<?> beanClass = bean.getClass();

        for (Method method : beanClass.getDeclaredMethods()) {
            BodyTypeMapping mapping = AnnotatedElementUtils
                    .findMergedAnnotation(method, BodyTypeMapping.class);

            if (mapping == null) {
                continue;
            }

            String[] paths = resolvePaths(mapping);
            if (paths.length == 0) {
                System.out.println("⚠️ 映射路径为空: " + beanClass.getSimpleName() + "#" + method.getName());
                continue;
            }

            // 创建映射注册信息
            MappingRegistration registration = new MappingRegistration(
                    bean,
                    method,
                    paths,
                    mapping
            );

            // 检查是否已经注册过（防止重复）
            String registrationKey = beanClass.getName() + "#" + method.getName();
            if (registeredMappings.containsKey(registrationKey)) {
                System.out.println("ℹ️ 映射已存在: " + registrationKey);
                continue;
            }

            // 延迟注册（等待Web上下文完全初始化）
            pendingRegistrations.add(registration);
            System.out.println("📋 待注册映射: " + beanClass.getSimpleName() + "#" + method.getName() +
                    " -> " + String.join(",", paths) + " (type=" + mapping.type() + ")");
        }
    }

    /**
     * 注册所有待处理的映射
     */
    private void registerPendingMappings(RequestMappingHandlerMapping handlerMapping) {
        if (pendingRegistrations.isEmpty()) {
            System.out.println("ℹ️ 没有待注册的映射");
            return;
        }

        System.out.println("🔄 开始注册 " + pendingRegistrations.size() + " 个映射...");

        for (MappingRegistration registration : pendingRegistrations) {
            try {
                registerMapping(handlerMapping, registration);

                // 标记为已注册
                String key = registration.getBean().getClass().getName() + "#" + registration.getMethod().getName();
                registeredMappings.put(key, true);

            } catch (Exception e) {
                System.err.println("❌ 注册映射失败: " + registration);
                e.printStackTrace();
            }
        }

        pendingRegistrations.clear();
        System.out.println("✅ 映射注册完成");
    }

    /**
     * 执行单个映射注册
     */
    private void registerMapping(RequestMappingHandlerMapping handlerMapping,
                                 MappingRegistration registration) throws Exception {
        // 构建RequestMappingInfo
        RequestMappingInfo.Builder builder = RequestMappingInfo
                .paths(registration.getPaths())
                .methods(registration.getMapping().method())
                .consumes(registration.getMapping().consumes());

        if (registration.getMapping().produces().length > 0) {
            builder = builder.produces(registration.getMapping().produces());
        }

        // 添加自定义条件
        BodyTypeCondition condition = new BodyTypeCondition(registration.getMapping().type());
        RequestMappingInfo mappingInfo = builder
                .customCondition(condition)
                .options(new RequestMappingInfo.BuilderConfiguration()).build();

        // 注册映射
        handlerMapping.registerMapping(mappingInfo, registration.getBean(), registration.getMethod());

        System.out.println("✅ 已注册: " + registration.getBean().getClass().getSimpleName() +
                "#" + registration.getMethod().getName() + " (type=" + registration.getMapping().type() + ")");
    }

    /**
     * 判断是否为控制器类
     */
    private boolean isController(Class<?> beanClass) {
        return AnnotatedElementUtils.hasAnnotation(beanClass,
                org.springframework.stereotype.Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(beanClass,
                        org.springframework.web.bind.annotation.RestController.class);
    }

    /**
     * 解析路径（处理别名）
     */
    private String[] resolvePaths(BodyTypeMapping mapping) {
        if (mapping.path().length > 0) {
            return mapping.path();
        } else if (mapping.value().length > 0) {
            return mapping.value();
        }
        return new String[0];
    }

    /**
     * 映射注册信息内部类
     */
    private static class MappingRegistration {
        private final Object bean;
        private final Method method;
        private final String[] paths;
        private final BodyTypeMapping mapping;

        public MappingRegistration(Object bean, Method method, String[] paths, BodyTypeMapping mapping) {
            this.bean = bean;
            this.method = method;
            this.paths = paths;
            this.mapping = mapping;
        }

        public Object getBean() {
            return bean;
        }

        public Method getMethod() {
            return method;
        }

        public String[] getPaths() {
            return paths;
        }

        public BodyTypeMapping getMapping() {
            return mapping;
        }

        @Override
        public String toString() {
            return bean.getClass().getSimpleName() + "#" + method.getName() +
                    " -> " + String.join(",", paths);
        }
    }

    /**
     * 获取注册状态统计
     */
    public int getPendingRegistrationsCount() {
        return pendingRegistrations.size();
    }

    public int getRegisteredMappingsCount() {
        return registeredMappings.size();
    }


}
