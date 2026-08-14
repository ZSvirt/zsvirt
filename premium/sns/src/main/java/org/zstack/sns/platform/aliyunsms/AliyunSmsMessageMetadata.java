package org.zstack.sns.platform.aliyunsms;

import java.util.List;

/**
 * Created by Qi Le on 2019-08-01
 */
public class AliyunSmsMessageMetadata {
    private String templateCode;
    private String smsSign;
    //for validate message use
    private List<String> phoneNumbers;

    private String EVENT_NAMESPACE;
    private String EVENT_NAME;
    private String EVENT_LABELS;
    private String EVENT_EMERGENCY_LEVEL;
    private String EVENT_RESOURCE_ID;
    private String EVENT_RESOURCE_NAME;
    private String EVENT_ERROR;
    private String EVENT_TIME;
    private String EVENT_SUBSCRIPTION_UUID;
    private String EVENT_ACCOUNT_UUID;
    private String EVENT_DATA_UUID;

    private String ALARM_NAME;
    private String ALARM_UUID;
    private String ALARM_CONDITION;
    private String ALARM_METRIC;
    private String ALARM_NAMESPACE;
    private String ALARM_THRESHOLD;
    private String ALARM_LABELS;
    private String ALARM_DURATION;
    private String ALARM_PREVIOUS_STATUS;
    private String ALARM_CURRENT_STATUS;
    private String ALARM_CURRENT_VALUE;
    private String ALARM_TIME;
    private String ALARM_RESOURCE_TYPE;
    private String ALARM_RESOURCE_ID;
    private String ALARM_RESOURCE_NAME;
    private String ALARM_ACCOUNT_UUID;
    private String ALARM_DATA_UUID;
    private String ALARM_EMERGENCY_LEVEL;

    public void generateValidateMessage(String content) {
        this.EVENT_NAMESPACE = content;
        this.EVENT_NAME = content;
        this.EVENT_LABELS = content;
        this.EVENT_EMERGENCY_LEVEL = content;
        this.EVENT_RESOURCE_ID = content;
        this.EVENT_RESOURCE_NAME = content;
        this.EVENT_ERROR = content;
        this.EVENT_TIME = content;
        this.EVENT_SUBSCRIPTION_UUID = content;
        this.EVENT_ACCOUNT_UUID = content;
        this.EVENT_DATA_UUID = content;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getSmsSign() {
        return smsSign;
    }

    public void setSmsSign(String smsSign) {
        this.smsSign = smsSign;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public String getEVENT_NAMESPACE() {
        return EVENT_NAMESPACE;
    }

    public void setEVENT_NAMESPACE(String EVENT_NAMESPACE) {
        this.EVENT_NAMESPACE = EVENT_NAMESPACE;
    }

    public String getEVENT_NAME() {
        return EVENT_NAME;
    }

    public void setEVENT_NAME(String EVENT_NAME) {
        this.EVENT_NAME = EVENT_NAME;
    }

    public String getEVENT_LABELS() {
        return EVENT_LABELS;
    }

    public void setEVENT_LABELS(String EVENT_LABELS) {
        this.EVENT_LABELS = EVENT_LABELS;
    }

    public String getEVENT_EMERGENCY_LEVEL() {
        return EVENT_EMERGENCY_LEVEL;
    }

    public void setEVENT_EMERGENCY_LEVEL(String EVENT_EMERGENCY_LEVEL) {
        this.EVENT_EMERGENCY_LEVEL = EVENT_EMERGENCY_LEVEL;
    }

    public String getEVENT_RESOURCE_ID() {
        return EVENT_RESOURCE_ID;
    }

    public void setEVENT_RESOURCE_ID(String EVENT_RESOURCE_ID) {
        this.EVENT_RESOURCE_ID = EVENT_RESOURCE_ID;
    }

    public String getEVENT_RESOURCE_NAME() {
        return EVENT_RESOURCE_NAME;
    }

    public void setEVENT_RESOURCE_NAME(String EVENT_RESOURCE_NAME) {
        this.EVENT_RESOURCE_NAME = EVENT_RESOURCE_NAME;
    }

    public String getEVENT_ERROR() {
        return EVENT_ERROR;
    }

    public void setEVENT_ERROR(String EVENT_ERROR) {
        this.EVENT_ERROR = EVENT_ERROR;
    }

    public String getEVENT_TIME() {
        return EVENT_TIME;
    }

    public void setEVENT_TIME(String EVENT_TIME) {
        this.EVENT_TIME = EVENT_TIME;
    }

    public String getEVENT_SUBSCRIPTION_UUID() {
        return EVENT_SUBSCRIPTION_UUID;
    }

    public void setEVENT_SUBSCRIPTION_UUID(String EVENT_SUBSCRIPTION_UUID) {
        this.EVENT_SUBSCRIPTION_UUID = EVENT_SUBSCRIPTION_UUID;
    }

    public String getEVENT_ACCOUNT_UUID() {
        return EVENT_ACCOUNT_UUID;
    }

    public void setEVENT_ACCOUNT_UUID(String EVENT_ACCOUNT_UUID) {
        this.EVENT_ACCOUNT_UUID = EVENT_ACCOUNT_UUID;
    }

    public String getEVENT_DATA_UUID() {
        return EVENT_DATA_UUID;
    }

    public void setEVENT_DATA_UUID(String EVENT_DATA_UUID) {
        this.EVENT_DATA_UUID = EVENT_DATA_UUID;
    }

    public String getALARM_NAME() {
        return ALARM_NAME;
    }

    public void setALARM_NAME(String ALARM_NAME) {
        this.ALARM_NAME = ALARM_NAME;
    }

    public String getALARM_UUID() {
        return ALARM_UUID;
    }

    public void setALARM_UUID(String ALARM_UUID) {
        this.ALARM_UUID = ALARM_UUID;
    }

    public String getALARM_CONDITION() {
        return ALARM_CONDITION;
    }

    public void setALARM_CONDITION(String ALARM_CONDITION) {
        this.ALARM_CONDITION = ALARM_CONDITION;
    }

    public String getALARM_METRIC() {
        return ALARM_METRIC;
    }

    public void setALARM_METRIC(String ALARM_METRIC) {
        this.ALARM_METRIC = ALARM_METRIC;
    }

    public String getALARM_NAMESPACE() {
        return ALARM_NAMESPACE;
    }

    public void setALARM_NAMESPACE(String ALARM_NAMESPACE) {
        this.ALARM_NAMESPACE = ALARM_NAMESPACE;
    }

    public String getALARM_THRESHOLD() {
        return ALARM_THRESHOLD;
    }

    public void setALARM_THRESHOLD(String ALARM_THRESHOLD) {
        this.ALARM_THRESHOLD = ALARM_THRESHOLD;
    }

    public String getALARM_LABELS() {
        return ALARM_LABELS;
    }

    public void setALARM_LABELS(String ALARM_LABELS) {
        this.ALARM_LABELS = ALARM_LABELS;
    }

    public String getALARM_DURATION() {
        return ALARM_DURATION;
    }

    public void setALARM_DURATION(String ALARM_DURATION) {
        this.ALARM_DURATION = ALARM_DURATION;
    }

    public String getALARM_PREVIOUS_STATUS() {
        return ALARM_PREVIOUS_STATUS;
    }

    public void setALARM_PREVIOUS_STATUS(String ALARM_PREVIOUS_STATUS) {
        this.ALARM_PREVIOUS_STATUS = ALARM_PREVIOUS_STATUS;
    }

    public String getALARM_CURRENT_STATUS() {
        return ALARM_CURRENT_STATUS;
    }

    public void setALARM_CURRENT_STATUS(String ALARM_CURRENT_STATUS) {
        this.ALARM_CURRENT_STATUS = ALARM_CURRENT_STATUS;
    }

    public String getALARM_CURRENT_VALUE() {
        return ALARM_CURRENT_VALUE;
    }

    public void setALARM_CURRENT_VALUE(String ALARM_CURRENT_VALUE) {
        this.ALARM_CURRENT_VALUE = ALARM_CURRENT_VALUE;
    }

    public String getALARM_TIME() {
        return ALARM_TIME;
    }

    public void setALARM_TIME(String ALARM_TIME) {
        this.ALARM_TIME = ALARM_TIME;
    }

    public String getALARM_RESOURCE_TYPE() {
        return ALARM_RESOURCE_TYPE;
    }

    public void setALARM_RESOURCE_TYPE(String ALARM_RESOURCE_TYPE) {
        this.ALARM_RESOURCE_TYPE = ALARM_RESOURCE_TYPE;
    }

    public String getALARM_RESOURCE_ID() {
        return ALARM_RESOURCE_ID;
    }

    public void setALARM_RESOURCE_ID(String ALARM_RESOURCE_ID) {
        this.ALARM_RESOURCE_ID = ALARM_RESOURCE_ID;
    }

    public String getALARM_RESOURCE_NAME() {
        return ALARM_RESOURCE_NAME;
    }

    public void setALARM_RESOURCE_NAME(String ALARM_RESOURCE_NAME) {
        this.ALARM_RESOURCE_NAME = ALARM_RESOURCE_NAME;
    }

    public String getALARM_ACCOUNT_UUID() {
        return ALARM_ACCOUNT_UUID;
    }

    public void setALARM_ACCOUNT_UUID(String ALARM_ACCOUNT_UUID) {
        this.ALARM_ACCOUNT_UUID = ALARM_ACCOUNT_UUID;
    }

    public String getALARM_DATA_UUID() {
        return ALARM_DATA_UUID;
    }

    public void setALARM_DATA_UUID(String ALARM_DATA_UUID) {
        this.ALARM_DATA_UUID = ALARM_DATA_UUID;
    }

    public String getALARM_EMERGENCY_LEVEL() {
        return ALARM_EMERGENCY_LEVEL;
    }

    public void setALARM_EMERGENCY_LEVEL(String ALARM_EMERGENCY_LEVEL) {
        this.ALARM_EMERGENCY_LEVEL = ALARM_EMERGENCY_LEVEL;
    }

    //    String stringLimiter(String origin, int len) {
//        if (origin == null || origin.length() <= len || len <= 2) {
//            return origin;
//        }
//        String ret = origin.substring(0, len - 2) + '…';
//        return ret;
//    }
}
