package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateHostNetworkInterfaceEvent extends APIEvent {

    private HostNetworkInterfaceInventory inventory;

    public APIUpdateHostNetworkInterfaceEvent() { super(null); }

    public APIUpdateHostNetworkInterfaceEvent(String apiId) {
        super(apiId);
    }

    public HostNetworkInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkInterfaceInventory inventory) {
        this.inventory = inventory;
    }

    public  static APIUpdateHostNetworkInterfaceEvent __example__() {
        APIUpdateHostNetworkInterfaceEvent event = new APIUpdateHostNetworkInterfaceEvent();
        HostNetworkInterfaceInventory inventory = new HostNetworkInterfaceInventory();

        inventory.setDescription("network");

        event.setInventory(inventory);
        return event;
    }
}
