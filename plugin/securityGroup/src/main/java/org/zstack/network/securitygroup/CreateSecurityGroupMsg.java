package org.zstack.network.securitygroup;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2022/3/4 16:11
 */
public class CreateSecurityGroupMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String accountUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
