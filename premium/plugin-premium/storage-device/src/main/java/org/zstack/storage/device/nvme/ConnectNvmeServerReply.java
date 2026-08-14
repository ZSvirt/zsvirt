package org.zstack.storage.device.nvme;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class ConnectNvmeServerReply extends MessageReply {
    List<NvmeTargetInventory> nvmeTargetInventories;

    public List<NvmeTargetInventory> getNvmeTargetInventories() {
        return nvmeTargetInventories;
    }

    public void setNvmeTargetInventories(List<NvmeTargetInventory> nvmeTargetInventories) {
        this.nvmeTargetInventories = nvmeTargetInventories;
    }
}
