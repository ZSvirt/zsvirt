package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

public class CleanUpStorageTrashOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    String primaryStorageUuid;
    boolean force;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

}
