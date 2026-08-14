package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleAccountRefInventory;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryRoleAccountRefReply extends APIQueryReply {
    private List<RoleAccountRefInventory> inventories;

    public List<RoleAccountRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<RoleAccountRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryRoleAccountRefReply __example__() {
        APIQueryRoleAccountRefReply reply = new APIQueryRoleAccountRefReply();
        reply.setInventories(list(RoleAccountRefInventory.__example__()));
        return reply;
    }
}
