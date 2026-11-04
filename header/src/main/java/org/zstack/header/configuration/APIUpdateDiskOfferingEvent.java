package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateDiskOfferingEvent extends APIEvent {
    private DiskOfferingInventory inventory;

    public APIUpdateDiskOfferingEvent() {
    }

    public APIUpdateDiskOfferingEvent(String apiId) {
        super(apiId);
    }

    public DiskOfferingInventory getInventory() {
        return inventory;
    }

    public void setInventory(DiskOfferingInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateDiskOfferingEvent __example__() {
        APIUpdateDiskOfferingEvent event = new APIUpdateDiskOfferingEvent();
        event.setInventory(DiskOfferingInventory.__example__());
        return event;
    }
}
