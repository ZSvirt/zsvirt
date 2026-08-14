package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL2NetworkReply extends APIQueryReply {
    private List<L2NetworkInventory> inventories;

    public List<L2NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2NetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL2NetworkReply __example__() {
        APIQueryL2NetworkReply reply = new APIQueryL2NetworkReply();
        L2VlanNetworkInventory net = new L2VlanNetworkInventory();

        net.setName("Test-Net");
        net.setVlan(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0");
        net.setType("L2VlanNetwork");

        reply.setInventories(Arrays.asList(net));
        return reply;
    }

}
