package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVmNicSecurityPolicyReply extends APIQueryReply {
    private List<VmNicSecurityPolicyInventory> inventories;

    public List<VmNicSecurityPolicyInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicSecurityPolicyInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmNicSecurityPolicyReply __example__() {
        APIQueryVmNicSecurityPolicyReply reply = new APIQueryVmNicSecurityPolicyReply();
        VmNicSecurityPolicyInventory inventory = new VmNicSecurityPolicyInventory();
        inventory.setVmNicUuid(uuid());
        inventory.setIngressPolicy("DROP");
        inventory.setEgressPolicy("ACCEPT");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(inventory));
        reply.setSuccess(true);
        return reply;
    }
}
