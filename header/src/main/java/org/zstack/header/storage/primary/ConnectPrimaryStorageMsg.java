package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class ConnectPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private boolean newAdded;

    public boolean isNewAdded() {
        return newAdded;
    }

    public void setNewAdded(boolean newAdded) {
        this.newAdded = newAdded;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
