package org.zstack.guesttools.advanced;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateVmCustomSpecificationEvent extends APIEvent {
    private VmCustomSpecificationInventory inventory;

    public APICreateVmCustomSpecificationEvent() {
        super(null);
    }

    public APICreateVmCustomSpecificationEvent(String apiId) {
        super(apiId);
    }

    public VmCustomSpecificationInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmCustomSpecificationInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVmCustomSpecificationEvent __example__() {
        APICreateVmCustomSpecificationEvent event = new APICreateVmCustomSpecificationEvent();
        event.setInventory(VmCustomSpecificationInventory.__example__());
        return event;
    }
}
