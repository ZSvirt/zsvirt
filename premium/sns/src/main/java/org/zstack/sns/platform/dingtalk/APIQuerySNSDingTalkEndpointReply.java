package org.zstack.sns.platform.dingtalk;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSDingTalkEndpointReply extends APIQueryReply {
    private List<SNSDingTalkEndpointInventory> inventories;

    public List<SNSDingTalkEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSDingTalkEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
