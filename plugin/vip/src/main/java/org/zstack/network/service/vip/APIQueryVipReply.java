package org.zstack.network.service.vip;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVipReply extends APIQueryReply {
    private List<VipInventory> inventories;

    public List<VipInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VipInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVipReply __example__() {
        APIQueryVipReply reply = new APIQueryVipReply();
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

        reply.setInventories(list(inventory));
        return reply;
    }

}
