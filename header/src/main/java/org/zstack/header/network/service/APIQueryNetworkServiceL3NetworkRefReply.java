package org.zstack.header.network.service;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryNetworkServiceL3NetworkRefReply extends APIQueryReply {
    private List<NetworkServiceL3NetworkRefInventory> inventories;

    public List<NetworkServiceL3NetworkRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<NetworkServiceL3NetworkRefInventory> inventories) {
        this.inventories = inventories;
    }


 
    public static APIQueryNetworkServiceL3NetworkRefReply __example__() {
        APIQueryNetworkServiceL3NetworkRefReply reply = new APIQueryNetworkServiceL3NetworkRefReply();
        NetworkServiceL3NetworkRefInventory ns = new NetworkServiceL3NetworkRefInventory();

        ns.setL3NetworkUuid(uuid());
        ns.setNetworkServiceProviderUuid(uuid());
        ns.setNetworkServiceType("PortForwarding");

        reply.setInventories(Arrays.asList(ns));

        return reply;
    }

}
