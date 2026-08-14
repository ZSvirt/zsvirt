package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipState;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin.ruan 2021/03/23
 */
@RestResponse(allTo = "inventories")
public class APIGetVpcAttachedVipReply extends APIReply {
    private List<VipInventory> inventories;

    public List<VipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VipInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcAttachedVipReply __example__() {
        APIGetVpcAttachedVipReply reply = new APIGetVpcAttachedVipReply();

        VipInventory inventory = new org.zstack.network.service.vip.VipInventory();
        inventory.setName("vip1");
        inventory.setL3NetworkUuid(uuid());
        inventory.setUuid(uuid());
        inventory.setGateway("192.168.0.1");
        inventory.setNetmask("255.255.0.0");
        inventory.setIp("192.168.0.100");
        inventory.setIpRangeUuid(uuid());
        inventory.setPeerL3NetworkUuids(Arrays.asList(uuid()));
        inventory.setState(VipState.Enabled.toString());

        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
