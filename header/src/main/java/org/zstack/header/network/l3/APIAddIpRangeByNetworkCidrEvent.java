package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

/**
 */
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
        List<IpRangeInventory> ipRanges = new ArrayList<>();
        IpRangeInventory ipRange = new IpRangeInventory();
        ipRanges.add(ipRange);

        ipRange.setName("Test-IPRange");
        ipRange.setL3NetworkUuid(uuid());
        ipRange.setNetworkCidr("192.168.10.0/24");

        event.setInventories(ipRanges);
        return event;
    }

}
