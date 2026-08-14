package org.zstack.core.thread;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface SyncThread {
    public int level() default 1; 
    public boolean compoundSignature() default false;
    public String signature() default "";
}
