package org.zstack.sns;

public interface SNSApplicationPlatformFactory {
    SNSApplicationPlatformVO createApplicationPlatform(SNSApplicationPlatformVO vo, APICreateSNSApplicationPlatformMsg msg);

    String getApplicationPlatformType();

    SNSApplicationPlatformInventory getSNSApplicationPlatformInventory(SNSApplicationPlatformVO vo);

    SNSApplicationPlatform getSNSApplicationPlatform(String uuid);
}
