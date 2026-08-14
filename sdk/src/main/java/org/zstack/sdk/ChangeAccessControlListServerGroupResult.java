package org.zstack.sdk;

import org.zstack.sdk.LoadBalancerListerAcl;

public class ChangeAccessControlListServerGroupResult {
    public LoadBalancerListerAcl inventory;
    public void setInventory(LoadBalancerListerAcl inventory) {
        this.inventory = inventory;
    }
    public LoadBalancerListerAcl getInventory() {
        return this.inventory;
    }

}
