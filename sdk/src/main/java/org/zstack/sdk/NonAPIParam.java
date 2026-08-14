package org.zstack.sdk;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by lining on 2017/12/11.
 */
@Target(java.lang.annotation.ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface NonAPIParam {

}
