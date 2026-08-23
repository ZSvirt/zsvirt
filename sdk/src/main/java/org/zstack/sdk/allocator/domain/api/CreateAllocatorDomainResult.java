package org.zstack.sdk.allocator.domain.api;

import org.zstack.sdk.allocator.domain.entity.AllocatorDomainInventory;

public class CreateAllocatorDomainResult {
    public AllocatorDomainInventory inventory;
    public void setInventory(AllocatorDomainInventory inventory) {
        this.inventory = inventory;
    }
    public AllocatorDomainInventory getInventory() {
        return this.inventory;
    }

}
