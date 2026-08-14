package org.zstack.core.eventlog;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryEventLogReply extends APIQueryReply {
    private List<EventLogInventory> inventories;

    public List<EventLogInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<EventLogInventory> inventories) {
        this.inventories = inventories;
    }
}
