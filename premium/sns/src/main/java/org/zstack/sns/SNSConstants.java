package org.zstack.sns;

public interface SNSConstants {
    String SERVICE_ID = "sns";
    String EMAIL_PLATFORM = "Email";
    String DINGTALK_PLATFORM = "DingTalk";
    String FEISHU_PLATFORM = "FeiShu";
    String WECOM_PLATFORM = "WeCom";
    String HTTP_PLATFORM = "HTTP";
    String MICROSOFT_TEAMS_PLATFORM = "MicrosoftTeams";
    String ALIYUNSMS_PLATFORM = "AliyunSms";
    String SNMP_PLATFORM = "SNMP";
    String ACTION_CATEGORY = "sns";

    String SYSTEM_PLATFORM = "system";
    String SYSTEM_PLATFORM_UUID = "02d24b9b0a7f4ee1846f15cda248ceb7";
    String SNS_GLOBAL_PROPERTY_UPDATE_REPORTER = "/sns/globalpropertyupdated";

    SNSApplicationPlatformType SYSTEM_PLATFORM_TYPE = new SNSApplicationPlatformType(SYSTEM_PLATFORM);
}
