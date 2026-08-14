package org.zstack.header.network.l2;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryL2VlanNetworkReply extends APIQueryReply {
    private List<L2VlanNetworkInventory> inventories;

    public List<L2VlanNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VlanNetworkInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryL2VlanNetworkReply __example__() {
        APIQueryL2VlanNetworkReply reply = new APIQueryL2VlanNetworkReply();
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
