package org.zstack.sns.platform.dingtalk;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSDingTalkAtPersonReply extends APIQueryReply {
    private List<SNSDingTalkAtPersonInventory> inventories;

    public List<SNSDingTalkAtPersonInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSDingTalkAtPersonInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySNSDingTalkAtPersonReply __example__() {
        APIQuerySNSDingTalkAtPersonReply reply = new APIQuerySNSDingTalkAtPersonReply();

        SNSDingTalkAtPersonInventory inv = new SNSDingTalkAtPersonInventory();
        inv.setUuid(uuid());
        inv.setPhoneNumber("13062689903");
        inv.setEndpointUuid(uuid());

        reply.setInventories(Collections.singletonList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
