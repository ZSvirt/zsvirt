package org.zstack.sns.platform.wecom;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSWeComAtPersonReply extends APIQueryReply {
    private List<SNSWeComAtPersonInventory> inventories;

    public List<SNSWeComAtPersonInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSWeComAtPersonInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySNSWeComAtPersonReply __example__() {
        APIQuerySNSWeComAtPersonReply reply = new APIQuerySNSWeComAtPersonReply();

        SNSWeComAtPersonInventory inv = new SNSWeComAtPersonInventory();
        inv.setUuid(uuid());
        inv.setEndpointUuid(uuid());
        inv.setUserId("zhang.san");

        reply.setInventories(Collections.singletonList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
