package org.zstack.allocator.domain.api;

import org.zstack.allocator.domain.entity.AllocatorDomainInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateAllocatorDomainEvent extends APIEvent {
    private AllocatorDomainInventory inventory;

    public APIUpdateAllocatorDomainEvent() {
        super(null);
    }

    public APIUpdateAllocatorDomainEvent(String apiId) {
        super(apiId);
    }

    public AllocatorDomainInventory getInventory() {
        return inventory;
    }

    public void setInventory(AllocatorDomainInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateAllocatorDomainEvent __example__() {
        APIUpdateAllocatorDomainEvent event = new APIUpdateAllocatorDomainEvent();
        event.setInventory(AllocatorDomainInventory.__example__());
        return event;
    }
}
