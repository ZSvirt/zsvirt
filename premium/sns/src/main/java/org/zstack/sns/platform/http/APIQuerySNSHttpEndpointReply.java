package org.zstack.sns.platform.http;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSHttpEndpointReply extends APIQueryReply {
    private List<SNSHttpEndpointInventory> inventories;

    public List<SNSHttpEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSHttpEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
