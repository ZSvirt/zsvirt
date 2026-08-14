package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/16/2015.
 */
@RestResponse(allTo = "inventory")
public class APIChangeInstanceOfferingEvent extends APIEvent {
    private VmInstanceInventory inventory;

    public APIChangeInstanceOfferingEvent() {
    }

    public APIChangeInstanceOfferingEvent(String apiId) {
        super(apiId);
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeInstanceOfferingEvent __example__() {
        APIChangeInstanceOfferingEvent event = new APIChangeInstanceOfferingEvent();


        return event;
    }

}
