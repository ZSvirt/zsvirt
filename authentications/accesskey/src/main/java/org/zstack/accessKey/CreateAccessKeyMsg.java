package org.zstack.accessKey;

import org.zstack.header.message.NeedReplyMessage;

public class CreateAccessKeyMsg extends NeedReplyMessage implements CreateAccessKey {
    private String resourceUuid;

    private String accountUuid;

    private String userUuid;

    private String description;

    private String accessKeyID;

    private String accessKeySecret;

    private String accessKeyType = AccessKeyType.User.toString();

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getAccessKeyID() {
        return accessKeyID;
    }

    public void setAccessKeyID(String accessKeyID) {
        this.accessKeyID = accessKeyID;
    }

    @Override
    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    @Override
    public String getAccessKeyType() {
        return accessKeyType;
    }

    public void setAccessKeyType(String accessKeyType) {
        if (accessKeyType == null || accessKeyType.trim().isEmpty()) {
            this.accessKeyType = AccessKeyType.User.toString();
            return;
        }
        this.accessKeyType = AccessKeyType.valueOf(accessKeyType).toString();
    }
}
