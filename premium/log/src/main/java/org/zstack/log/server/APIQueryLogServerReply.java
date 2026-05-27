package org.zstack.log.server;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryLogServerReply extends APIQueryReply {

    private List<LogServerInventory> inventories;

    public List<LogServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LogServerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryLogServerReply __example__() {
        APIQueryLogServerReply reply = new APIQueryLogServerReply();
        return reply;
    }
}
