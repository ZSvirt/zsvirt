package org.zstack.header.scheduler;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by MaJin on 2019/5/15.
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface CascadeUpdate {
    boolean disableWhenEmpty() default false;

    Class resourceType();
}
