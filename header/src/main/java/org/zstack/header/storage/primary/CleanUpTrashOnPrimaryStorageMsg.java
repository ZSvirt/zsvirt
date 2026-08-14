package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Create by weiwang at 2019-04-18
 */
public class CleanUpTrashOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String uuid;

    private Long trashId;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getTrashId() {
        return trashId;
    }

    public void setTrashId(Long trashId) {
        this.trashId = trashId;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }
}
