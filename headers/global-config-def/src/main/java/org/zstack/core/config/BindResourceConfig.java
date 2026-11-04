package org.zstack.core.config;

import org.zstack.header.vo.ResourceVODefinition;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({java.lang.annotation.ElementType.FIELD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface BindResourceConfig {
    Class<? extends ResourceVODefinition>[] value();
}
