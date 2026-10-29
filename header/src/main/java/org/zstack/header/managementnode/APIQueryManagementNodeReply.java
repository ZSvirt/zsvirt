package org.zstack.header.managementnode;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryManagementNodeReply extends APIQueryReply {
    private List<ManagementNodeInventory> inventories;

    public List<ManagementNodeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ManagementNodeInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryManagementNodeReply __example__() {
        APIQueryManagementNodeReply reply = new APIQueryManagementNodeReply();
        reply.setInventories(list(ManagementNodeInventory.__example__()));
        return reply;
    }
}
