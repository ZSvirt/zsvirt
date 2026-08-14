package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 27/05/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVtepReply extends APIQueryReply {
    private List<VtepInventory> inventories;

    public List<VtepInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VtepInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVtepReply __example__() {
        APIQueryVtepReply reply = new APIQueryVtepReply();
        VtepInventory inv = new VtepInventory();

        inv.setUuid(uuid());
        inv.setHostUuid(uuid());
        inv.setVtepIp("192.168.100.10");
        inv.setPort(4789);
        inv.setType("KVM_HOST_VXLAN");

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }
}
