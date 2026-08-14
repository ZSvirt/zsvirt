package org.zstack.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2017/6/12.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface TriggerEventFactoryMethod {
    String value();
}
