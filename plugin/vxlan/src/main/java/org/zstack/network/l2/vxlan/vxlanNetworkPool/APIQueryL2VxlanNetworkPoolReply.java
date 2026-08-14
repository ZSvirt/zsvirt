package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 15/03/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryL2VxlanNetworkPoolReply extends APIQueryReply {
    private List<L2VxlanNetworkPoolInventory> inventories;

    public List<L2VxlanNetworkPoolInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2VxlanNetworkPoolInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryL2VxlanNetworkPoolReply __example__() {
        APIQueryL2VxlanNetworkPoolReply reply = new APIQueryL2VxlanNetworkPoolReply();
        L2VxlanNetworkPoolInventory net = new L2VxlanNetworkPoolInventory();

        net.setName("Test-Net");
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setType("L2VxlanNetworkPool");

        reply.setInventories(Arrays.asList(net));
        return reply;
    }
}
