package org.zstack.guesttools;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin on 2023-03-14.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateGuestToolsStateReply extends APIReply {

    GuestToolsStateInventory inventory;

    public APIUpdateGuestToolsStateReply() {
    }

    public static APIUpdateGuestToolsStateReply __example__() {
        APIUpdateGuestToolsStateReply reply = new APIUpdateGuestToolsStateReply();

        GuestToolsStateInventory inventory = new GuestToolsStateInventory();
        inventory.setVmInstanceUuid(uuid());
        inventory.setQgaState(GuestToolsQgaState.Running.toString());
        inventory.setZwatchState(GuestToolsZWatchState.Running.toString());
        inventory.setOsType("Kylin");
        inventory.setPlatform("Kylin");
        inventory.setVersion("4.6.11");

        reply.setInventory(inventory);

        return reply;
    }

    public GuestToolsStateInventory getInventory() {
        return inventory;
    }

    public void setInventory(GuestToolsStateInventory inventory) {
        this.inventory = inventory;
    }
}
