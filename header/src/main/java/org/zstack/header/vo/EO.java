package org.zstack.header.vo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EO {
    Class<?> EOClazz();

    String softDeletedColumn() default "deleted";

    boolean needView() default true;
}
