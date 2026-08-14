package org.zstack.header.acl;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@RestResponse(allTo = "inventory")
public class APIAddAccessControlListEntryEvent extends APIEvent {
    private AccessControlListEntryInventory inventory;

    public APIAddAccessControlListEntryEvent() { }

    public APIAddAccessControlListEntryEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(AccessControlListEntryInventory inventory) {
        this.inventory = inventory;
    }

    public AccessControlListEntryInventory getInventory() {
        return inventory;
    }

    public static APIAddAccessControlListEntryEvent __example__() {
        APIAddAccessControlListEntryEvent event = new APIAddAccessControlListEntryEvent();
        AccessControlListEntryInventory inv = new AccessControlListEntryInventory();

        inv.setAclUuid(uuid());
        inv.setIpEntries("192.168.48.0/24");

        event.setInventory(inv);
        return event;
    }
}

