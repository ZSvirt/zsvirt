package org.zstack.header.message;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface GsonTransient {
    boolean transientInHttpResponse() default true;
}
