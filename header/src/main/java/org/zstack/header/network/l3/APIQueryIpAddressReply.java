package org.zstack.header.network.l3;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.network.IPv6Constants;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryIpAddressReply extends APIQueryReply {
    private List<UsedIpInventory> inventories;

    public List<UsedIpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UsedIpInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryIpAddressReply __example__() {
        APIQueryIpAddressReply reply = new APIQueryIpAddressReply();
        UsedIpInventory ip = new UsedIpInventory();

        ip.setVmNicUuid(uuid());
        ip.setUuid(uuid());
        ip.setL3NetworkUuid(uuid());
        ip.setGateway("192.168.1.1");
        ip.setIpRangeUuid(uuid());
        ip.setIpVersion(IPv6Constants.IPv4);
        ip.setIp("192.168.1.100");
        ip.setNetmask("255.255.255.0");

        reply.setInventories(Arrays.asList(ip));
        return reply;
    }

}
