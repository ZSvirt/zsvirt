package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryIpRangeReply extends APIQueryReply {
    private List<IpRangeInventory> inventories;

    public List<IpRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<IpRangeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryIpRangeReply __example__() {
        APIQueryIpRangeReply reply = new APIQueryIpRangeReply();
        IpRangeInventory ipRange = new IpRangeInventory();

        ipRange.setName("Test-IPRange");
        ipRange.setL3NetworkUuid(uuid());
        ipRange.setNetworkCidr("192.168.10.0/24");

        reply.setInventories(Arrays.asList(ipRange));
        return reply;
    }

}
