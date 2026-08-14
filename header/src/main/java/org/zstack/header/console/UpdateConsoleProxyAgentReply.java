package org.zstack.header.console;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 2018-09-13.
 */
public class UpdateConsoleProxyAgentReply extends MessageReply {
    private ConsoleProxyAgentInventory inventory;

    public ConsoleProxyAgentInventory getInventory() {
        return inventory;
    }

    public void setInventory(ConsoleProxyAgentInventory inventory) {
        this.inventory = inventory;
    }
}
