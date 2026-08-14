package org.zstack.zwatch.alarm.sns;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSTextTemplateReply extends APIQueryReply {
    private List<SNSTextTemplateInventory> inventories;

    public List<SNSTextTemplateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSTextTemplateInventory> inventories) {
        this.inventories = inventories;
    }
}
