package org.zstack.header.cbt;

import org.zstack.header.message.NeedReplyMessage;

public class EnableCbtTaskMsg extends NeedReplyMessage {
    private String uuid;
    private String bitmapName;
    private String accountUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBitmapName() {
        return bitmapName;
    }

    public void setBitmapName(String bitmapName) {
        this.bitmapName = bitmapName;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
