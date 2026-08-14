package org.zstack.header.core.validation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Validation {
    boolean notNull() default true;

    boolean notZero() default false;
}
