package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.ldap.entity.LdapServerInventory;

@RestResponse(allTo = "inventory")
public class APIAddLdapServerEvent extends APIEvent {
    private LdapServerInventory inventory;

    public APIAddLdapServerEvent(String apiId) {
        super(apiId);
    }

    public APIAddLdapServerEvent() {
        super(null);
    }

    public LdapServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddLdapServerEvent __example__() {
        APIAddLdapServerEvent event = new APIAddLdapServerEvent();
        event.setInventory(LdapServerInventory.__example__());
        return event;
    }

}
