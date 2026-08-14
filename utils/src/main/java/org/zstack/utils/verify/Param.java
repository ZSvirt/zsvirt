package org.zstack.utils.verify;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Param {
    boolean required() default true;

    boolean noTrim() default false;

    long[] numberRange() default {};

    int maxLength() default Integer.MAX_VALUE;

    int minLength() default 0;

    Class resourceType() default Object.class;

    String[] validValues() default {};
}