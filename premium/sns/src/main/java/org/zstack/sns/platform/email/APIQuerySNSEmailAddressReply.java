package org.zstack.sns.platform.email;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySNSEmailAddressReply extends APIQueryReply {
    private List<SNSEmailAddressInventory> inventories;

    public List<SNSEmailAddressInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSEmailAddressInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySNSEmailAddressReply __example__() {
        APIQuerySNSEmailAddressReply reply = new APIQuerySNSEmailAddressReply();

        SNSEmailAddressInventory inv = new SNSEmailAddressInventory();
        inv.setUuid(uuid());
        inv.setEndpointUuid(uuid());
        inv.setEmailAddress("test@zstack.io");
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());

        reply.setInventories(Collections.singletonList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
