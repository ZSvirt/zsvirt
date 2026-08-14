package org.zstack.header.network.l2;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;
import java.sql.Timestamp;

@RestResponse(fieldsTo = "all")
public class APIGetCandidateL2NetworksForAttachingClusterReply extends APIReply {
    private List<L2NetworkData> inventories;

    public List<L2NetworkData> getInventories() {
        return inventories;
    }

    public void setInventories(List<L2NetworkData> inventories) {
        this.inventories = inventories;
    }

    public static APIGetCandidateL2NetworksForAttachingClusterReply __example__() {
        APIGetCandidateL2NetworksForAttachingClusterReply reply = new APIGetCandidateL2NetworksForAttachingClusterReply();
        L2NetworkData date = new L2NetworkData();
        date.setName("l2");
        date.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        date.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        date.setDescription("test");
        date.setPhysicalInterface("eth0");

        reply.setInventories(asList(date));

        return reply;
    }
}
