package org.zstack.core.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by frank on 1/18/2016.
 */
@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface GlobalConfigDef {
    Class type() default String.class;
    String defaultValue() default "";
    String description() default "";
    String validatorRegularExpression() default "";
}
