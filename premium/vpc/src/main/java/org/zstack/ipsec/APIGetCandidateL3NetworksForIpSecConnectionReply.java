package org.zstack.ipsec;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @author: shixin.ruan
 * @date: 2021-01-04
 **/
@RestResponse(allTo = "inventories")
public class APIGetCandidateL3NetworksForIpSecConnectionReply extends APIReply {
    private List<L3NetworkInventory> inventories;

    public List<L3NetworkInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<L3NetworkInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetCandidateL3NetworksForIpSecConnectionReply __example__() {
        APIGetCandidateL3NetworksForIpSecConnectionReply reply = new APIGetCandidateL3NetworksForIpSecConnectionReply();
        L3NetworkInventory l3 = new L3NetworkInventory();
        l3.setName("Test-L3Network");
        l3.setL2NetworkUuid(uuid());
        reply.setInventories(Arrays.asList(l3));
        return reply;
    }
}
