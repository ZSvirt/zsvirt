package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListVmNicInSecurityGroupReply extends APIReply {
    private List<VmNicSecurityGroupRefInventory> inventories;

    public List<VmNicSecurityGroupRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicSecurityGroupRefInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListVmNicInSecurityGroupReply __example__() {
        APIListVmNicInSecurityGroupReply reply = new APIListVmNicInSecurityGroupReply();
        //deprecated
        return reply;
    }

}
