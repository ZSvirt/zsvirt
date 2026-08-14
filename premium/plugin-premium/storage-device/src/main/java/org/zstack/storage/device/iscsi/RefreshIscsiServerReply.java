package org.zstack.storage.device.iscsi;

import org.zstack.header.message.MessageReply;

public class RefreshIscsiServerReply extends MessageReply {
    IscsiServerInventory iscsiServerInventory;

    public IscsiServerInventory getIscsiServerInventory() {
        return iscsiServerInventory;
    }

    public void setIscsiServerInventory(IscsiServerInventory iscsiServerInventory) {
        this.iscsiServerInventory = iscsiServerInventory;
    }
}
