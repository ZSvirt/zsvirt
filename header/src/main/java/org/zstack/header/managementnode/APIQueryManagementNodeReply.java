package org.zstack.header.managementnode;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
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
        ManagementNodeInventory inventory = new ManagementNodeInventory();
        inventory.setUuid(uuid());
        inventory.setHostName("127.0.0.1");
        inventory.setJoinDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setHeartBeat(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(list(inventory));
        return reply;
    }

}
