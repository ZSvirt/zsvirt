package org.zstack.zwatch.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.migratedb.EventRecordsInventory;

import java.util.List;


@RestResponse(allTo = "inventories")
public class APIQueryEventRecordReply extends APIQueryReply {
    private List<EventRecordsInventory> inventories;

    public List<EventRecordsInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EventRecordsInventory> inventories) {
        this.inventories = inventories;
    }
}
