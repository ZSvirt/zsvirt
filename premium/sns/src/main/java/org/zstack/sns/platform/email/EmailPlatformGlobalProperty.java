package org.zstack.sns.platform.email;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class EmailPlatformGlobalProperty {
    @GlobalProperty(name = "SNS.addEmailPlatform.connectTimeout", defaultValue = "5000")
    public static int SNS_ADD_EMAIL_PLATFORM_CONNECT_TIMEOUT;

    @GlobalProperty(name = "SNS.addEmailPlatform.readTimeout", defaultValue = "5000")
    public static int SNS_ADD_EMAIL_PLATFORM_READ_TIMEOUT;
}
