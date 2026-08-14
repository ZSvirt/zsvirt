package org.zstack.sns;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSApplicationEndpointReply extends APIQueryReply {
    private List<SNSApplicationEndpointInventory> inventories;

    public List<SNSApplicationEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSApplicationEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
