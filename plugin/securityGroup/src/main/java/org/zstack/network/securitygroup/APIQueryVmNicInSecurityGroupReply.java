package org.zstack.network.securitygroup;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVmNicInSecurityGroupReply extends APIQueryReply {
    private List<VmNicSecurityGroupRefInventory> inventories;

    public List<VmNicSecurityGroupRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicSecurityGroupRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryVmNicInSecurityGroupReply __example__() {
        APIQueryVmNicInSecurityGroupReply reply = new APIQueryVmNicInSecurityGroupReply();
        VmNicSecurityGroupRefInventory secRefInv = new VmNicSecurityGroupRefInventory();
        secRefInv.setUuid(uuid());
        secRefInv.setSecurityGroupUuid(uuid());
        secRefInv.setVmInstanceUuid(uuid());
        secRefInv.setVmNicUuid(uuid());
        secRefInv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        secRefInv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(secRefInv));
        reply.setSuccess(true);
        return reply;
    }

}
