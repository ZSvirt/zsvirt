package org.zstack.storage.device.localRaid;

import org.zstack.header.message.MessageReply;

/**
 * author:kaicai.hu
 * Date:2021/9/13
 */
public class LocateLocalRaidPhysicalDriveReply extends MessageReply {
    private RaidPhysicalDriveInventory inventory;

    public RaidPhysicalDriveInventory getInventory() {
        return inventory;
    }

    public void setInventory(RaidPhysicalDriveInventory inventory) {
        this.inventory = inventory;
    }
}
