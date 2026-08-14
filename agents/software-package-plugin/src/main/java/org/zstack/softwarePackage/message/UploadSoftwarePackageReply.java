package org.zstack.softwarePackage.message;

import org.zstack.header.message.MessageReply;
import org.zstack.softwarePackage.header.SoftwarePackageInventory;

public class UploadSoftwarePackageReply extends MessageReply {
    private SoftwarePackageInventory inventory;

    public SoftwarePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(SoftwarePackageInventory inventory) {
        this.inventory = inventory;
    }
}