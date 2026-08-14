package org.zstack.sns.platform.feishu;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSFeiShuAtPersonReply extends APIQueryReply {
    private List<SNSFeiShuAtPersonInventory> inventories;

    public List<SNSFeiShuAtPersonInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSFeiShuAtPersonInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySNSFeiShuAtPersonReply __example__() {
        APIQuerySNSFeiShuAtPersonReply reply = new APIQuerySNSFeiShuAtPersonReply();

        SNSFeiShuAtPersonInventory inv = new SNSFeiShuAtPersonInventory();
        inv.setUuid(uuid());
        inv.setUuid("zhang.san");
        inv.setEndpointUuid(uuid());

        reply.setInventories(Collections.singletonList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
