package org.zstack.sns.platform.feishu;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSFeiShuEndpointReply extends APIQueryReply {
    private List<SNSFeiShuEndpointInventory> inventories;

    public List<SNSFeiShuEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSFeiShuEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
