package org.zstack.header.identity;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 7/15/2015.
 */
public class AccountDeletionMsg extends NeedReplyMessage implements AccountMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccountUuid() {
        return uuid;
    }
}
