package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;

import java.util.Arrays;

@RestResponse(allTo = "inventory")
public class APISetIpOnHostNetworkBondingEvent extends APIEvent {

    private HostNetworkBondingInventory inventory;

    public APISetIpOnHostNetworkBondingEvent() { super(null); }

    public APISetIpOnHostNetworkBondingEvent(String apiId) {
        super(apiId);
    }

    public HostNetworkBondingInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostNetworkBondingInventory inventory) {
        this.inventory = inventory;
    }

    public  static APISetIpOnHostNetworkBondingEvent __example__() {
        APISetIpOnHostNetworkBondingEvent event = new APISetIpOnHostNetworkBondingEvent();
        HostNetworkBondingInventory inventory = new HostNetworkBondingInventory();

        inventory.setHostUuid(uuid());
        inventory.setIpAddresses(Arrays.asList("192.168.1.1".split(",")));

        event.setInventory(inventory);
        return event;
    }
}
