package org.zstack.sdk;

import org.zstack.sdk.SshKeyPairInventory;

public class AttachSshKeyPairToVmInstanceResult {
    public SshKeyPairInventory inventory;
    public void setInventory(SshKeyPairInventory inventory) {
        this.inventory = inventory;
    }
    public SshKeyPairInventory getInventory() {
        return this.inventory;
    }

}
