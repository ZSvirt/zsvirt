package org.zstack.iam1.api.ensemble;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.ensemble.ResourceEnsembleInventory;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
@RestResponse(allTo = "inventory")
public class APIGetResourceEnsembleMembersReply extends APIQueryReply {
    private ResourceEnsembleInventory inventory;

    public ResourceEnsembleInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceEnsembleInventory inventory) {
        this.inventory = inventory;
    }

    public static APIGetResourceEnsembleMembersReply __example__() {
        APIGetResourceEnsembleMembersReply reply = new APIGetResourceEnsembleMembersReply();
        reply.setInventory(ResourceEnsembleInventory.__example__());
        return reply;
    }
}
