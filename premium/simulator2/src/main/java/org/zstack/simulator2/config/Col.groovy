package org.zstack.simulator2.config

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by xing5 on 2017/9/18.
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface Col {
    Class parent() default Object.class
    String parentKey() default "id"
    boolean notNull() default true
}