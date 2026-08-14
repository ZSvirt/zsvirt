package org.zstack.sns;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQuerySNSTopicSubscriberReply extends APIQueryReply {
    public static APIQuerySNSTopicSubscriberReply __example__() {
        APIQuerySNSTopicSubscriberReply ret = new APIQuerySNSTopicSubscriberReply();
        ret.inventories = asList(SNSSubscriberInventory.__example__());
        return ret;
    }

    private List<SNSSubscriberInventory> inventories;

    public List<SNSSubscriberInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSSubscriberInventory> inventories) {
        this.inventories = inventories;
    }
}
