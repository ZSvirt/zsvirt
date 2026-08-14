package org.zstack.sns.platform.wecom;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSWeComEndpointReply extends APIQueryReply {
    private List<SNSWeComEndpointInventory> inventories;

    public List<SNSWeComEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSWeComEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
