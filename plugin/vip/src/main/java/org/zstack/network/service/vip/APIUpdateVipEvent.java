package org.zstack.network.service.vip;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Created by frank on 6/15/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateVipEvent extends APIEvent {
    private VipInventory inventory;

    public VipInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateVipEvent() {
    }

    public APIUpdateVipEvent(String apiId) {
        super(apiId);
    }
 
    public static APIUpdateVipEvent __example__() {
        APIUpdateVipEvent event = new APIUpdateVipEvent();
        VipInventory inventory = new VipInventory();
        inventory.setName("new name");
        inventory.setL3NetworkUuid(uuid());
        inventory.setUuid(uuid());
        inventory.setGateway("127.0.0.1");
        inventory.setNetmask("255.255.0.0");
        inventory.setIp("192.168.0.1");
        inventory.setIpRangeUuid(uuid());
        inventory.setPeerL3NetworkUuids(Arrays.asList(uuid()));
        inventory.setState(VipState.Enabled.toString());

        event.setInventory(inventory);

        return event;
    }

}
