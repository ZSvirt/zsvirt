package org.zstack.sns;

public interface SNSManager {
    SNSApplicationPlatformFactory getSNSApplicationPlatformFactory(String type);

    SNSApplicationEndpointFactory getSNSApplicationEndpointFactory(String type);
}
