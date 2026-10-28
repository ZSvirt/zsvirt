package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateDiskOfferingEvent extends APIEvent {
    private DiskOfferingInventory inventory;

    public APICreateDiskOfferingEvent() {
        super(null);
    }

    public APICreateDiskOfferingEvent(String apiId) {
        super(apiId);
    }

    public DiskOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }

 
    public static APICreateDiskOfferingEvent __example__() {
        APICreateDiskOfferingEvent event = new APICreateDiskOfferingEvent();
        event.setInventory(DiskOfferingInventory.__example__());
        return event;
    }
}
