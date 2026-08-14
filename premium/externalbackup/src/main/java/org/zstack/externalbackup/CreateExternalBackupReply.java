package org.zstack.externalbackup;

import org.zstack.header.message.MessageReply;

/**
 * Created by MaJin on 2019/12/3.
 */
public class CreateExternalBackupReply extends MessageReply {
    private ExternalBackupInventory inventory;

    public ExternalBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(ExternalBackupInventory inventory) {
        this.inventory = inventory;
    }
}
