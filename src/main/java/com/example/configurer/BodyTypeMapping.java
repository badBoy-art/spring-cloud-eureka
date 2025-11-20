package com.example.configurer;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 3️⃣ 组合注解：一次性声明 path、type、method、consumes 等属性
 * 与普通 @RequestMapping 不同，它会把手动的 RequestMappingInfo 注册过程交给 Registrar。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BodyTypeMapping {

    String[] path() default {};

    /** Shortcut for path – can be used as {@code @BodyTypeMapping("/api/process")}. */
    String[] value() default {};

    /** HTTP methods – default POST. */
    RequestMethod[] method() default { RequestMethod.POST };

    /** Required consumes – default application/json. */
    String[] consumes() default { "application/json" };

    /** Optional produces – default empty (Spring will decide). */
    String[] produces() default {};

    /** The value that must appear in the request‑body JSON field “type”. */
    String type();

}
