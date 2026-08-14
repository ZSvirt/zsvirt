package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 15/03/2017.
 */
@RestResponse(allTo = "inventories")
public class APIQueryVniRangeReply extends APIQueryReply {

    private List<VniRangeInventory> inventories;

    public List<VniRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VniRangeInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVniRangeReply __example__() {
        APIQueryVniRangeReply reply = new APIQueryVniRangeReply();
        VniRangeInventory range = new VniRangeInventory();

        range.setName("Test-Range");
        range.setDescription("Test");
        range.setL2NetworkUuid(uuid());
        range.setStartVni(10);
        range.setEndVni(10000);

        reply.setInventories(Arrays.asList(range));
        return reply;
    }
}
