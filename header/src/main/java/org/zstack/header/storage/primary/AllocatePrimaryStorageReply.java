package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

/**
 * Allocate PrimaryStorage reply
 * * @deprecated
 * * This class is no longer acceptable to allocate PrimaryStorage.
 * * <p> Use {@link AllocatePrimaryStorageSpaceReply} instead.
 */
@Deprecated
public class AllocatePrimaryStorageReply extends MessageReply {
    private PrimaryStorageInventory primaryStorageInventory;
    private long size;

    public AllocatePrimaryStorageReply(PrimaryStorageInventory primaryStorageInventory) {
        super();
        this.primaryStorageInventory = primaryStorageInventory;
    }

    public PrimaryStorageInventory getPrimaryStorageInventory() {
        return primaryStorageInventory;
    }

    public void setPrimaryStorageInventory(PrimaryStorageInventory primaryStorageInventory) {
        this.primaryStorageInventory = primaryStorageInventory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
