package org.zstack.header.longjob;

import org.zstack.header.message.APIMessage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseApiTimeout {
    Class<? extends APIMessage> value();
}