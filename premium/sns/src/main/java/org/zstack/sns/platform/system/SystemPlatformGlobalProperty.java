package org.zstack.sns.platform.system;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class SystemPlatformGlobalProperty {
    @GlobalProperty(name = "sns.systemTopic.endpoints.http.url", defaultValue = "")
    public static String systemTopicHttpEndpointURL;
    @GlobalProperty(name = "sns.systemTopic.endpoints.http.url.username", defaultValue = "")
    public static String systemTopicHttpEndpointURLUsername;
    @GlobalProperty(name = "sns.systemTopic.endpoints.http.url.password", defaultValue = "")
    public static String systemTopicHttpEndpointURLPassword;
}
