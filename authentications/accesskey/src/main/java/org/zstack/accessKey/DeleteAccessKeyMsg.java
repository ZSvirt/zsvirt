package org.zstack.accessKey;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteAccessKeyMsg extends NeedReplyMessage {
    private String accountUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
