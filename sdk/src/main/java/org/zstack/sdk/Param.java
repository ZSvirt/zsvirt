package org.zstack.sdk;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2016/12/9.
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Param {
    boolean required() default true;

    String[] validValues() default {};

    String validRegexValues() default "";

    int maxLength() default Integer.MIN_VALUE;

    int minLength() default Integer.MIN_VALUE;

    boolean nonempty() default false;

    boolean nullElements() default false;

    boolean emptyString() default true;

    long[] numberRange() default {};

    String[] numberRangeUnit() default {};

    boolean noTrim() default false;
}
