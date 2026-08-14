package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;

public class APIDetachNetworkServiceProviderFromL2NetworkEvent extends APIEvent {
    private NetworkServiceProviderInventory inventory;

    public APIDetachNetworkServiceProviderFromL2NetworkEvent() {
        super(null);
    }

    public APIDetachNetworkServiceProviderFromL2NetworkEvent(String apiId) {
        super(apiId);
    }

    public NetworkServiceProviderInventory getInventory() {
        return inventory;
    }

    public void setInventory(NetworkServiceProviderInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachNetworkServiceProviderFromL2NetworkEvent __example__() {
        APIDetachNetworkServiceProviderFromL2NetworkEvent event = new APIDetachNetworkServiceProviderFromL2NetworkEvent();


        return event;
    }

}
