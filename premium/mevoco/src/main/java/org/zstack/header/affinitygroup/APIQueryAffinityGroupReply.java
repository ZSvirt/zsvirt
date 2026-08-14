package org.zstack.header.affinitygroup;

import org.zstack.core.Platform;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;


/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventories")
public class APIQueryAffinityGroupReply extends APIQueryReply {
    private List<AffinityGroupInventory> inventories;

    public List<AffinityGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AffinityGroupInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAffinityGroupReply __example__() {
        APIQueryAffinityGroupReply reply = new APIQueryAffinityGroupReply();
        AffinityGroupInventory inv = new AffinityGroupInventory();
        inv.setUuid(uuid());
        inv.setName("affinity-group-test");
        inv.setDescription("affinity group for test");
        inv.setPolicy(AffinityGroupPolicy.ANTISOFT.toString());
        inv.setType(AffinityGroupType.HOST.toString());
        inv.setVersion("1.0");

        AffinityGroupUsageInventory usageInv = new AffinityGroupUsageInventory();
        usageInv.setResourceUuid(inv.getUuid());
        usageInv.setResourceType(AffinityGroupType.HOST.toString());
        usageInv.setResourceUuid(uuid());

        inv.setUsages(asList(usageInv));

        reply.setInventories(asList(inv));

        return reply;

    }

}
