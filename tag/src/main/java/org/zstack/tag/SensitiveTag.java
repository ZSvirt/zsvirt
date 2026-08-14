package org.zstack.tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by MaJin on 2020/9/3.
 */

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveTag {
    String[] tokens() default {};

    Class<? extends SensitiveTagOutputHandler> customizeOutput() default SensitiveTokenTagOutputHandler.class;
}
