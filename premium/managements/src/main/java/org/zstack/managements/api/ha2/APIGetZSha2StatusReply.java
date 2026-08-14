package org.zstack.managements.api.ha2;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.managements.entity.ha2.ZSha2StatusView;

@RestResponse(fieldsTo = "all")
public class APIGetZSha2StatusReply extends APIReply {
    private ZSha2StatusView inventory;

    public ZSha2StatusView getInventory() {
        return inventory;
    }

    public void setInventory(ZSha2StatusView inventory) {
        this.inventory = inventory;
    }

    public static APIGetZSha2StatusReply __example__() {
        APIGetZSha2StatusReply reply = new APIGetZSha2StatusReply();
        reply.setInventory(ZSha2StatusView.__example__());
        return reply;
    }
}
