package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vpc.VpcConstants;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 20/11/2017
 */
@RestResponse(allTo = "inventories")
public class APIGetAttachableVpcL3NetworkReply extends APIReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetAttachableVpcL3NetworkReply __example__() {
        APIGetAttachableVpcL3NetworkReply reply = new APIGetAttachableVpcL3NetworkReply();

        L3NetworkInventory inventory = new L3NetworkInventory();
        inventory.setName("test-l3");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setDescription("test l3");
        inventory.setL2NetworkUuid(uuid());
        inventory.setSystem(false);
        inventory.setUuid(uuid());
        inventory.setZoneUuid(uuid());
        inventory.setCategory(L3NetworkCategory.Private.toString());
        inventory.setType(VpcConstants.VPC_L3_NETWORK_TYPE);

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
