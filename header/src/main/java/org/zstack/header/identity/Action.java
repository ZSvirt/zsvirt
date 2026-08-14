package org.zstack.header.identity;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by frank on 7/9/2015.
 */
@Target(java.lang.annotation.ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Action {
    boolean adminOnly() default false;

    boolean accountOnly() default false;

    String category();

    String[] names() default {};

    boolean accountControl() default false;
}
