package org.zstack.sns.platform.http;

import org.zstack.sns.*;

public class SNSSystemHttpEndpointFactory extends SNSHttpEndpointFactory {
    public static final SNSApplicationEndpointType type = new SNSApplicationEndpointType("SYSTEM_HTTP");

    @Override
    public String getApplicationEndpointType() {
        return type.toString();
    }
}
