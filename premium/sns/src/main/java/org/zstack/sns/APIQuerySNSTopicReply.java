package org.zstack.sns;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQuerySNSTopicReply extends APIQueryReply {
    private List<SNSTopicInventory> inventories;

    public static APIQuerySNSTopicReply __example__() {
        APIQuerySNSTopicReply reply = new APIQuerySNSTopicReply();
        reply.setInventories(asList(SNSTopicInventory.__example__()));
        return reply;
    }

    public List<SNSTopicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSTopicInventory> inventories) {
        this.inventories = inventories;
    }
}
