package org.zstack.directory;

import org.zstack.header.message.MessageReply;

/**
 * @author shenjin
 * @date 2022/12/1 14:24
 */
public class CreateDirectoryReply extends MessageReply {
    private DirectoryInventory inventory;

    public DirectoryInventory getInventory() {
        return inventory;
    }

    public void setInventory(DirectoryInventory inventory) {
        this.inventory = inventory;
    }
}
