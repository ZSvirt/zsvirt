package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIGetAttachablePublicL3ForVRouterReply extends APIReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetAttachablePublicL3ForVRouterReply __example__() {
        APIGetAttachablePublicL3ForVRouterReply reply = new APIGetAttachablePublicL3ForVRouterReply();

        L3NetworkInventory inventory = new L3NetworkInventory();
        inventory.setName("test-pub-l3");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setDescription("test pub l3");
        inventory.setL2NetworkUuid(uuid());
        inventory.setSystem(true);
        inventory.setUuid(uuid());
        inventory.setZoneUuid(uuid());

        IpRangeInventory ipRangeInventory = new IpRangeInventory();
        ipRangeInventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ipRangeInventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ipRangeInventory.setStartIp("100.64.0.10");
        ipRangeInventory.setEndIp("100.64.0.100");
        ipRangeInventory.setName("test ip range");
        ipRangeInventory.setNetworkCidr("100.64.0.0/24");
        ipRangeInventory.setNetmask("255.255.255.0");
        ipRangeInventory.setGateway("100.64.0.1");
        ipRangeInventory.setL3NetworkUuid(uuid());

        inventory.setIpRanges(Arrays.asList(ipRangeInventory));

        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
