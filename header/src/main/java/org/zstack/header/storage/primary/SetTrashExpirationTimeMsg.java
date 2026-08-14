package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class SetTrashExpirationTimeMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String uuid;
    private int expirationTime;

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setExpirationTime(int expirationTime) {
        this.expirationTime = expirationTime;
    }

    public int getExpirationTime() {
        return expirationTime;
    }
}
