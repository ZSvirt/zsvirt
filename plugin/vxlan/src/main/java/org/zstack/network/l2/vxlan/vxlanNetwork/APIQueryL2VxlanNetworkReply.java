package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 15/03/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryL2VxlanNetworkReply extends APIQueryReply {
    private List<L2VxlanNetworkInventory> inventories;

    public List<L2VxlanNetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VxlanNetworkInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryL2VxlanNetworkReply __example__() {
        APIQueryL2VxlanNetworkReply reply = new APIQueryL2VxlanNetworkReply();
        L2VxlanNetworkInventory net = new L2VxlanNetworkInventory();

        net.setName("Test-Net");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setType("L2VxlanNetwork");

        reply.setInventories(Arrays.asList(net));
        return reply;
    }
}
