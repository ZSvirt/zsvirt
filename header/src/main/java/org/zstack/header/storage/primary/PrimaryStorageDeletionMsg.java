package org.zstack.header.storage.primary;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class PrimaryStorageDeletionMsg extends DeletionMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
