package org.zstack.snmp.agent;

import org.zstack.header.message.MessageReply;

/**
 *
 * @Author : jingwang
 * @create 2023/7/24 11:37 AM
 */
public class StartSnmpAgentReply extends MessageReply {
    private SnmpAgentInventory inventory;

    public SnmpAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(SnmpAgentInventory inventory) {
        this.inventory = inventory;
    }
}
