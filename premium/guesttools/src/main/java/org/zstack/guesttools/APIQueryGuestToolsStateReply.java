package org.zstack.guesttools;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryGuestToolsStateReply extends APIQueryReply {
    private List<GuestToolsStateInventory> inventories;

    public List<GuestToolsStateInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<GuestToolsStateInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryGuestToolsStateReply __example__() {
        APIQueryGuestToolsStateReply reply = new APIQueryGuestToolsStateReply();
        return reply;
    }
}