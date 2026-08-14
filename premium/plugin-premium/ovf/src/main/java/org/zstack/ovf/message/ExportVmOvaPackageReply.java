package org.zstack.ovf.message;

import org.zstack.header.message.MessageReply;
import org.zstack.ovf.datatype.ImagePackageInventory;

/**
 * Created by Qi Le on 2022/5/5
 */
public class ExportVmOvaPackageReply extends MessageReply {
    private ImagePackageInventory inventory;

    public ImagePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImagePackageInventory inventory) {
        this.inventory = inventory;
    }
}
