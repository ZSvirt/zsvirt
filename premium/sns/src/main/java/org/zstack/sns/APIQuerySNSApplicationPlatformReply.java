package org.zstack.sns;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQuerySNSApplicationPlatformReply extends APIQueryReply {
    private List<SNSApplicationPlatformInventory> inventories;

    public static APIQuerySNSApplicationPlatformReply __example__() {
        APIQuerySNSApplicationPlatformReply reply = new APIQuerySNSApplicationPlatformReply();
        reply.setInventories(asList(SNSApplicationPlatformInventory.__example__()));
        return reply;
    }

    public List<SNSApplicationPlatformInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSApplicationPlatformInventory> inventories) {
        this.inventories = inventories;
    }
}
