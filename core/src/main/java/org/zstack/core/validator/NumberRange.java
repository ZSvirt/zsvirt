package org.zstack.core.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(value = {ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NumberRange {
    long min() default Long.MIN_VALUE;
    long max() default Long.MAX_VALUE;
}
