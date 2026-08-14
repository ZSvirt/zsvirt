package org.zstack.sdk;

import org.zstack.sdk.LoadBalancerInventory;

public class RefreshLoadBalancerResult {
    public LoadBalancerInventory inventory;
    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
    public LoadBalancerInventory getInventory() {
        return this.inventory;
    }

}
