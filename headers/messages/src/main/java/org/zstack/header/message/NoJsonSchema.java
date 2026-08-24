package org.zstack.header.message;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 */
@Target({java.lang.annotation.ElementType.FIELD, ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NoJsonSchema {
}
