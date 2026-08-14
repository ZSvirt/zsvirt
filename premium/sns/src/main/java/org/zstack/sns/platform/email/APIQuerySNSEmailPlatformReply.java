package org.zstack.sns.platform.email;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSEmailPlatformReply extends APIQueryReply {
    private List<SNSEmailPlatformInventory> inventories;

    public List<SNSEmailPlatformInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSEmailPlatformInventory> inventories) {
        this.inventories = inventories;
    }
}
