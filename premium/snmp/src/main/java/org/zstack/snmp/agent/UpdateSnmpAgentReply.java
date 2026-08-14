package org.zstack.snmp.agent;

import org.zstack.header.message.MessageReply;

/**
 *
 * @Author : jingwang
 * @create 2023/7/26 3:27 PM
 */
public class UpdateSnmpAgentReply extends MessageReply {
    private SnmpAgentInventory inventory;

    public SnmpAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(SnmpAgentInventory inventory) {
        this.inventory = inventory;
    }
}
