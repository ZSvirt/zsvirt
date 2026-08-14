package org.zstack.zwatch.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.migratedb.AuditsInventory;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryAuditReply extends APIQueryReply {
    private List<AuditsInventory> inventories;

    public List<AuditsInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AuditsInventory> inventories) {
        this.inventories = inventories;
    }
}
