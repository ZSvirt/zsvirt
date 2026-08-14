package org.zstack.managements.api.common;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.managements.entity.common.ManagementsStatusView;

@RestResponse(fieldsTo = "all")
public class APIGetManagementNodesStatusReply extends APIReply {
    private ManagementsStatusView inventory;

    public ManagementsStatusView getInventory() {
        return inventory;
    }

    public void setInventory(ManagementsStatusView inventory) {
        this.inventory = inventory;
    }

    public static APIGetManagementNodesStatusReply __example__() {
        APIGetManagementNodesStatusReply reply = new APIGetManagementNodesStatusReply();
        reply.setInventory(ManagementsStatusView.__example__());
        return reply;
    }
}
