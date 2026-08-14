package org.zstack.header.affinitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import static org.zstack.utils.CollectionDSL.list;

import java.util.List;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.header.affinitygroup
 * @date 2021/1/12 1:20 PM
 */
@RestResponse(fieldsTo = "all")
public class APIGetCandidateAffinityGroupForAttachingVmReply  extends APIReply {

    private List<AffinityGroupInventory> inventories;

    public List<AffinityGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AffinityGroupInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetCandidateAffinityGroupForAttachingVmReply __example__() {
        APIGetCandidateAffinityGroupForAttachingVmReply reply = new APIGetCandidateAffinityGroupForAttachingVmReply();

        AffinityGroupInventory affinityGroup = new AffinityGroupInventory();
        affinityGroup.setUuid(uuid());
        affinityGroup.setName("affinity-group-test");
        affinityGroup.setDescription("affinity group for test");
        affinityGroup.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
        affinityGroup.setType(AffinityGroupType.HOST.toString());
        affinityGroup.setVersion("1.0");
        reply.setInventories(list(affinityGroup));

        return reply;
    }
}
