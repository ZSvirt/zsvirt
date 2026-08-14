package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryRoleReply extends APIQueryReply {
    private List<RoleInventory> inventories;

    public List<RoleInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<RoleInventory> inventories) {
        this.inventories = inventories;
    }
}
