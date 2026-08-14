package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;


public class APIAddNetworkServiceProviderEvent extends APIEvent {
    private NetworkServiceProviderInventory inventory;

    public NetworkServiceProviderInventory getInventory() {
        return inventory;
    }

    public void setInventory(NetworkServiceProviderInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddNetworkServiceProviderEvent __example__() {
        APIAddNetworkServiceProviderEvent event = new APIAddNetworkServiceProviderEvent();


        return event;
    }

}
