package org.zstack.core.db;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 */
@Target(java.lang.annotation.ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface DeadlockAutoRestart {
}
