package org.zstack.header.acl;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIChangeAccessControlListRedirectRuleEvent extends APIEvent {
    private AccessControlListEntryInventory inventory;

    public APIChangeAccessControlListRedirectRuleEvent() { }

    public APIChangeAccessControlListRedirectRuleEvent(String apiId) {
        super(apiId);
    }

    public void setInventory(AccessControlListEntryInventory inventory) {
        this.inventory = inventory;
    }

    public AccessControlListEntryInventory getInventory() {
        return inventory;
    }

    public static APIChangeAccessControlListRedirectRuleEvent __example__() {
        APIChangeAccessControlListRedirectRuleEvent event = new APIChangeAccessControlListRedirectRuleEvent();
        AccessControlListEntryInventory inv = new AccessControlListEntryInventory();

        inv.setName("acl-group");
        inv.setAclUuid(uuid());
        inv.setType("RedirectRule");
        inv.setDomain("zstack.io");
        inv.setUrl("/test");
        event.setInventory(inv);
        return event;
    }
}
