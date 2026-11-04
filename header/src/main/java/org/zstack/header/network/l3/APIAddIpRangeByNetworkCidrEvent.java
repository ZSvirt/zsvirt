package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(fieldsTo = {"all"})
public class APIAddIpRangeByNetworkCidrEvent extends APIEvent {
    private  IpRangeInventory inventory;

    private List<IpRangeInventory> inventories;

    public IpRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(IpRangeInventory ipRangeInventory) {
        this.inventory = ipRangeInventory;
    }

    public List<IpRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IpRangeInventory> inventories) {
        this.inventories = inventories;
    }

    public APIAddIpRangeByNetworkCidrEvent(String apiId) {
        super(apiId);
    }

    public APIAddIpRangeByNetworkCidrEvent() {
        super(null);
    }
 
    public static APIAddIpRangeByNetworkCidrEvent __example__() {
        APIAddIpRangeByNetworkCidrEvent event = new APIAddIpRangeByNetworkCidrEvent();
        event.setInventories(list(IpRangeInventory.__example__()));
        return event;
    }
}
