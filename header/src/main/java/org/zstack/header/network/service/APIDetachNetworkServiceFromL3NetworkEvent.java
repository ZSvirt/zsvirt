package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Created by frank on 1/4/2016.
 */
@RestResponse(allTo = "inventory")
public class APIDetachNetworkServiceFromL3NetworkEvent extends APIEvent {
    private L3NetworkInventory inventory;

    public APIDetachNetworkServiceFromL3NetworkEvent() {
    }

    public APIDetachNetworkServiceFromL3NetworkEvent(String apiId) {
        super(apiId);
    }

    public L3NetworkInventory getInventory() {
        return inventory;
    }

    public void setInventory(L3NetworkInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIDetachNetworkServiceFromL3NetworkEvent __example__() {
        APIDetachNetworkServiceFromL3NetworkEvent event = new APIDetachNetworkServiceFromL3NetworkEvent();
        L3NetworkInventory l3 = new L3NetworkInventory();
        NetworkServiceL3NetworkRefInventory ns = new NetworkServiceL3NetworkRefInventory();

        l3.setUuid(uuid());
        l3.setNetworkServices(Arrays.asList(ns));

        return event;
    }

}
