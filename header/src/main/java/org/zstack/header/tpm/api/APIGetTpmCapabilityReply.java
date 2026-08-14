package org.zstack.header.tpm.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tpm.entity.TpmCapabilityView;

@RestResponse(fieldsTo = "all")
public class APIGetTpmCapabilityReply extends APIReply {
    private TpmCapabilityView inventory;

    public TpmCapabilityView getInventory() {
        return inventory;
    }

    public void setInventory(TpmCapabilityView inventory) {
        this.inventory = inventory;
    }

    public static APIGetTpmCapabilityReply __example__() {
        APIGetTpmCapabilityReply reply = new APIGetTpmCapabilityReply();
        reply.setInventory(TpmCapabilityView.__example__());
        return reply;
    }
}
