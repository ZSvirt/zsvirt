package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateHostKernelInterfaceEvent extends APIEvent {

    private HostKernelInterfaceInventory inventory;

    public APICreateHostKernelInterfaceEvent(String apiId) {
        super(apiId);
    }

    public APICreateHostKernelInterfaceEvent() {
        super(null);
    }

    public HostKernelInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostKernelInterfaceInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateHostKernelInterfaceEvent __example__() {
        APICreateHostKernelInterfaceEvent event = new APICreateHostKernelInterfaceEvent();
        event.setInventory(HostKernelInterfaceInventory.__example__());
        return event;
    }

}
