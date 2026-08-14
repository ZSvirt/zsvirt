package org.zstack.guesttools;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-09-19.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetLatestGuestToolsForVmReply extends APIReply {
    private GuestToolsInventory inventory;

    public GuestToolsInventory getInventory() {
        return inventory;
    }

    public void setInventory(GuestToolsInventory inventory) {
        this.inventory = inventory;
    }

    public static APIGetLatestGuestToolsForVmReply __example__() {
        APIGetLatestGuestToolsForVmReply reply = new APIGetLatestGuestToolsForVmReply();
        reply.setInventory(GuestToolsInventory.__example__());
        return reply;
    }
}
