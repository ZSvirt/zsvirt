package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;

import java.util.List;

public class APIListSecurityGroupReply extends APIReply {
    private List<SecurityGroupInventory> inventories;

    public List<SecurityGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityGroupInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIListSecurityGroupReply __example__() {
        APIListSecurityGroupReply reply = new APIListSecurityGroupReply();
        //deprecated
        return reply;
    }

}
