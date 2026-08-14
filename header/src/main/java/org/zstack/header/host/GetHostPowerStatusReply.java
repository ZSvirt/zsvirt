package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * @Author : jingwang
 * @create 2023/5/6 2:04 PM
 */
public class GetHostPowerStatusReply extends MessageReply {
    HostIpmiInventory inventory;

    public HostIpmiInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostIpmiInventory inventory) {
        this.inventory = inventory;
    }
}
