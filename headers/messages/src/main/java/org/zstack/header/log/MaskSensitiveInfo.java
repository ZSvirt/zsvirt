package org.zstack.header.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * mask sensitive on REST even if CoreGlobalProperty.MASK_SENSITIVE_INFO is set to false
 * see also {@link NoLogging}
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaskSensitiveInfo {
}
