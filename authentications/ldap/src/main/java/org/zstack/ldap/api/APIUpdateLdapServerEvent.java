package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.ldap.entity.LdapServerInventory;

@RestResponse(allTo = "inventory")
public class APIUpdateLdapServerEvent extends APIEvent {
    private LdapServerInventory inventory;

    public APIUpdateLdapServerEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateLdapServerEvent() {
        super(null);
    }

    public LdapServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateLdapServerEvent __example__() {
        APIUpdateLdapServerEvent event = new APIUpdateLdapServerEvent();
        event.setInventory(LdapServerInventory.__example__());
        return event;
    }
}
