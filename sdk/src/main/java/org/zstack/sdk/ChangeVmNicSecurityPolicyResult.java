package org.zstack.sdk;

import org.zstack.sdk.VmNicSecurityPolicyInventory;

public class ChangeVmNicSecurityPolicyResult {
    public VmNicSecurityPolicyInventory inventory;
    public void setInventory(VmNicSecurityPolicyInventory inventory) {
        this.inventory = inventory;
    }
    public VmNicSecurityPolicyInventory getInventory() {
        return this.inventory;
    }

}
