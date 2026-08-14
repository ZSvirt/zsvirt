package org.zstack.guesttools.advanced;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateVmCustomSpecificationEvent extends APIEvent {
    private VmCustomSpecificationInventory inventory;

    public VmCustomSpecificationInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmCustomSpecificationInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateVmCustomSpecificationEvent() {
        super(null);
    }

    public APIUpdateVmCustomSpecificationEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateVmCustomSpecificationEvent __example__() {
        APIUpdateVmCustomSpecificationEvent event = new APIUpdateVmCustomSpecificationEvent();
        event.setInventory(VmCustomSpecificationInventory.__example__());
        return event;
    }
}
