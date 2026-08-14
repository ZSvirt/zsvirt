package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by MaJin on 2017-08-19.
 */
public class AllocatePrimaryStorageDryRunReply extends MessageReply {
    private List<PrimaryStorageInventory> primaryStorageInventories;

    public void setPrimaryStorageInventories(List<PrimaryStorageInventory> primaryStorageInventories) {
        this.primaryStorageInventories = primaryStorageInventories;
    }

    public List<PrimaryStorageInventory> getPrimaryStorageInventories() {
        return primaryStorageInventories;
    }
}
