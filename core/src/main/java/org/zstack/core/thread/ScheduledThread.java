package org.zstack.core.thread;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(java.lang.annotation.ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ScheduledThread {
    public long interval();
    public TimeUnit timeUnit() default TimeUnit.SECONDS;
    public long delay() default 0;
}
