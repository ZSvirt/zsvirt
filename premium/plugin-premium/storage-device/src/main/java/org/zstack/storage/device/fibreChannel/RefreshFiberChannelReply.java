package org.zstack.storage.device.fibreChannel;

import org.zstack.header.message.MessageReply;
import org.zstack.storage.device.iscsi.IscsiServerInventory;

import java.util.List;

public class RefreshFiberChannelReply extends MessageReply {
    List<FiberChannelStorageInventory> fiberChannelStorageInventories;

    public List<FiberChannelStorageInventory> getFiberChannelStorageInventories() {
        return fiberChannelStorageInventories;
    }

    public void setFiberChannelStorageInventories(List<FiberChannelStorageInventory> fiberChannelStorageInventories) {
        this.fiberChannelStorageInventories = fiberChannelStorageInventories;
    }
}
