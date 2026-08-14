package org.zstack.header.core;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by xing5 on 2016/9/10.
 */
@Target(java.lang.annotation.ElementType.METHOD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface BypassWhenUnitTest {
}
