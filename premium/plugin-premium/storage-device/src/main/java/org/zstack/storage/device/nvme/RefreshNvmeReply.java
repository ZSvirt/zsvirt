package org.zstack.storage.device.nvme;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class RefreshNvmeReply extends MessageReply {
    List<NvmeTargetInventory> fiberChannelStorageInventories;

    public List<NvmeTargetInventory> getFiberChannelStorageInventories() {
        return fiberChannelStorageInventories;
    }

    public void setFiberChannelStorageInventories(List<NvmeTargetInventory> fiberChannelStorageInventories) {
        this.fiberChannelStorageInventories = fiberChannelStorageInventories;
    }
}
