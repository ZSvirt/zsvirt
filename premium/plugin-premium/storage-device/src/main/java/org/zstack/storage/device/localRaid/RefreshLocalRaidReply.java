package org.zstack.storage.device.localRaid;

import org.zstack.header.message.MessageReply;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageInventory;

import java.util.List;

public class RefreshLocalRaidReply extends MessageReply {
    List<RaidControllerInventory> controllers;

    public List<RaidControllerInventory> getControllers() {
        return controllers;
    }

    public void setControllers(List<RaidControllerInventory> controllers) {
        this.controllers = controllers;
    }
}
