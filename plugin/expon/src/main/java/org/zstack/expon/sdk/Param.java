package org.zstack.expon.sdk;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Param {
    boolean required() default true;

    String[] validValues() default {};

    long[] numberRange() default {};

    boolean nonempty() default false;
}
