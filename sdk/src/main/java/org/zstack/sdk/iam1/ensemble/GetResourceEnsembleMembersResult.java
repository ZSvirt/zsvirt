package org.zstack.sdk.iam1.ensemble;

import org.zstack.sdk.iam1.ensemble.ResourceEnsembleInventory;

public class GetResourceEnsembleMembersResult {
    public ResourceEnsembleInventory inventory;
    public void setInventory(ResourceEnsembleInventory inventory) {
        this.inventory = inventory;
    }
    public ResourceEnsembleInventory getInventory() {
        return this.inventory;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

}
