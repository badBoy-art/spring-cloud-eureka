package com.example.service.monitor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author xuedui.zhao
 * @create 2018-12-27
 */
@Aspect
@Component
public class MonitorAdvice {

    @Pointcut("execution (* com.example.service.Speakable.*(..))")
    public void pointcut() {
    }

    @Around("pointcut()")

    public void around(ProceedingJoinPoint pjp) throws Throwable {

        MonitorSession.begin(pjp.getSignature().getName());
        pjp.proceed();
        MonitorSession.end();
    }
}
