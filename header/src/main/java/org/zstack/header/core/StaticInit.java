package org.zstack.header.core;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2016/6/1.
 */
@Target(java.lang.annotation.ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface StaticInit {
    int order() default 0;
}
