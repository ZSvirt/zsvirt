package org.zstack.vpc;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipState;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shixin.ruan 2021/03/23
 */
@RestResponse(allTo = "inventories")
public class APIGetVpcAttachedEipReply extends APIReply {
    private List<EipInventory> inventories;

    public List<EipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EipInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVpcAttachedEipReply __example__() {
        APIGetVpcAttachedEipReply reply = new APIGetVpcAttachedEipReply();

        EipInventory inventory = new EipInventory();
        inventory.setVipUuid(uuid());
        inventory.setVmNicUuid(uuid());
        inventory.setName("Test-EIP");

        reply.setInventories(Arrays.asList(inventory));
        return reply;
    }
}
