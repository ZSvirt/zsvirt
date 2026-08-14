package org.zstack.network.service.lb;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @author: sulin.sheng
 * @date: 2021-11-24
 **/
@RestResponse(allTo = "inventories")
public class APIGetCandidateL3NetworksForServerGroupReply extends APIReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetCandidateL3NetworksForServerGroupReply __example__() {
        APIGetCandidateL3NetworksForServerGroupReply reply = new APIGetCandidateL3NetworksForServerGroupReply();
        L3NetworkInventory l3 = new L3NetworkInventory();
        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());
        reply.setInventories(Arrays.asList(l3));
        return reply;
    }
}
