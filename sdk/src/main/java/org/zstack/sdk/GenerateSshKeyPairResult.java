package org.zstack.sdk;

import org.zstack.sdk.SshPrivateKeyPairInventory;

public class GenerateSshKeyPairResult {
    public SshPrivateKeyPairInventory inventory;
    public void setInventory(SshPrivateKeyPairInventory inventory) {
        this.inventory = inventory;
    }
    public SshPrivateKeyPairInventory getInventory() {
        return this.inventory;
    }

}
