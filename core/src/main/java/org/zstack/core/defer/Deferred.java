package org.zstack.core.defer;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(java.lang.annotation.ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Deferred {
    Class<? extends Throwable>[] rollbackForClass() default {};
    Class<? extends Throwable>[] noRollbackForClass() default {};
}
