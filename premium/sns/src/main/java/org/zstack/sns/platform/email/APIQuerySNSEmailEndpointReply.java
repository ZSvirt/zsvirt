package org.zstack.sns.platform.email;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSEmailEndpointReply extends APIQueryReply {
    private List<SNSEmailEndpointInventory> inventories;

    public List<SNSEmailEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSEmailEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
