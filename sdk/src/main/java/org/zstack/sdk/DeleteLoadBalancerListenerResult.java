package org.zstack.sdk;

import org.zstack.sdk.LoadBalancerInventory;

public class DeleteLoadBalancerListenerResult {
    public LoadBalancerInventory inventory;
    public void setInventory(LoadBalancerInventory inventory) {
        this.inventory = inventory;
    }
    public LoadBalancerInventory getInventory() {
        return this.inventory;
    }

}
