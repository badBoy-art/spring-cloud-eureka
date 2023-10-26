package com.example.resolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)//作用范围（方法）
@Retention(RetentionPolicy.RUNTIME)//作用时间（运行时）
public @interface IP {

}
