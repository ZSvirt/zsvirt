package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.MessageReply;

/**
 * Created by frank on 8/11/2015.
 */
public class CreateVirtualRouterVmReply extends MessageReply {
    private VirtualRouterVmInventory inventory;

    public VirtualRouterVmInventory getInventory() {
        return inventory;
    }

    public void setInventory(VirtualRouterVmInventory inventory) {
        this.inventory = inventory;
    }
}
