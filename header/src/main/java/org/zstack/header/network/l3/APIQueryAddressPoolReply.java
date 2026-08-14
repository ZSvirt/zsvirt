package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryAddressPoolReply extends APIQueryReply {
    private List<AddressPoolInventory> inventories;

    public List<AddressPoolInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AddressPoolInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAddressPoolReply __example__() {
        APIQueryAddressPoolReply reply = new APIQueryAddressPoolReply();
        AddressPoolInventory ipRange = new AddressPoolInventory();

        ipRange.setName("Test-IPRange");
        ipRange.setL3NetworkUuid(uuid());
        ipRange.setNetworkCidr("192.168.10.0/24");
        ipRange.setIpRangeType(IpRangeType.AddressPool);

        reply.setInventories(Arrays.asList(ipRange));
        return reply;
    }

}
