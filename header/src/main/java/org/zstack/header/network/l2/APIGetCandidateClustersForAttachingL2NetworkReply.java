package org.zstack.header.network.l2;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;
import static org.zstack.utils.CollectionDSL.list;

@RestResponse(fieldsTo = "all")
public class APIGetCandidateClustersForAttachingL2NetworkReply extends APIReply {
    private List<ClusterInventory> inventories;

    public List<ClusterInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ClusterInventory> inventories) {
        this.inventories = inventories;
    }
    public static APIGetCandidateClustersForAttachingL2NetworkReply __example__() {
        APIGetCandidateClustersForAttachingL2NetworkReply reply = new APIGetCandidateClustersForAttachingL2NetworkReply();
        reply.setInventories(list(ClusterInventory.__example__()));
        return reply;
    }
}
