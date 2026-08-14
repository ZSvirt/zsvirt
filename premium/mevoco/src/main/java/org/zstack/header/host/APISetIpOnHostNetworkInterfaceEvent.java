package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.util.Arrays;

@RestResponse(allTo = "inventory")
public class APISetIpOnHostNetworkInterfaceEvent extends APIEvent {

    private HostNetworkInterfaceInventory inventory;

    public APISetIpOnHostNetworkInterfaceEvent() { super(null); }

    public APISetIpOnHostNetworkInterfaceEvent(String apiId) {
        super(apiId);
    }

    public HostNetworkInterfaceInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkInterfaceInventory inventory) {
        this.inventory = inventory;
    }

    public  static APISetIpOnHostNetworkInterfaceEvent __example__() {
        APISetIpOnHostNetworkInterfaceEvent event = new APISetIpOnHostNetworkInterfaceEvent();
        HostNetworkInterfaceInventory inventory = new HostNetworkInterfaceInventory();

        inventory.setHostUuid(uuid());
        inventory.setIpAddresses(Arrays.asList("192.168.1.1".split(",")));

        event.setInventory(inventory);
        return event;
    }
}
