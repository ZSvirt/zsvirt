package org.zstack.header.network.service;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListNetworkServiceProviderReply extends APIReply {
    private List<NetworkServiceProviderInventory> inventories;

    public List<NetworkServiceProviderInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkServiceProviderInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListNetworkServiceProviderReply __example__() {
        APIListNetworkServiceProviderReply reply = new APIListNetworkServiceProviderReply();


        return reply;
    }

}
