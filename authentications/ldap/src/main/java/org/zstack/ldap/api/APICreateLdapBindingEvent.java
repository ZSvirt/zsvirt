package org.zstack.ldap.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory;

@RestResponse(allTo = "inventory")
public class APICreateLdapBindingEvent extends APIEvent {
    private AccountThirdPartyAccountSourceRefInventory inventory;

    public APICreateLdapBindingEvent(String apiId) {
        super(apiId);
    }

    public APICreateLdapBindingEvent() {
        super(null);
    }

    public AccountThirdPartyAccountSourceRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountThirdPartyAccountSourceRefInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateLdapBindingEvent __example__() {
        APICreateLdapBindingEvent event = new APICreateLdapBindingEvent();
        AccountThirdPartyAccountSourceRefInventory inventory = new AccountThirdPartyAccountSourceRefInventory();
        inventory.setId(1L);
        inventory.setCredentials("ou=Employee,uid=test");
        inventory.setAccountUuid(uuid());
        inventory.setAccountSourceUuid(uuid());

        event.setInventory(inventory);
        return event;
    }

}
