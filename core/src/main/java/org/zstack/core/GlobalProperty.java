package org.zstack.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 */
@Target(value = {ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface GlobalProperty {
    final static String DEFAULT_NULL_STRING = "very-magic-zstack-default-null-value-you-can-not-use-this$#%@#$%@#$%";

    String name();
    String defaultValue() default DEFAULT_NULL_STRING;
    String[] defaultListValue() default {};
    boolean required() default false;
    boolean encrypted() default false;
}
