package org.zstack.allocator.domain.api;

import org.zstack.allocator.domain.entity.AllocatorDomainInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateAllocatorDomainEvent extends APIEvent {
    private AllocatorDomainInventory inventory;

    public APICreateAllocatorDomainEvent() {
        super(null);
    }

    public APICreateAllocatorDomainEvent(String apiId) {
        super(apiId);
    }

    public AllocatorDomainInventory getInventory() {
        return inventory;
    }

    public void setInventory(AllocatorDomainInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateAllocatorDomainEvent __example__() {
        APICreateAllocatorDomainEvent event = new APICreateAllocatorDomainEvent();
        event.setInventory(AllocatorDomainInventory.__example__());
        return event;
    }
}
