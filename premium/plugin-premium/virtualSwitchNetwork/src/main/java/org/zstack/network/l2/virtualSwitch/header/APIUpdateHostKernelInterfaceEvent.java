package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateHostKernelInterfaceEvent extends APIEvent {

    private HostKernelInterfaceInventory inventory;

    public APIUpdateHostKernelInterfaceEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateHostKernelInterfaceEvent() {
        super(null);
    }

    public HostKernelInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostKernelInterfaceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateHostKernelInterfaceEvent __example__() {
        APIUpdateHostKernelInterfaceEvent event = new APIUpdateHostKernelInterfaceEvent();
        event.setInventory(HostKernelInterfaceInventory.__example__());
        return event;
    }

}
