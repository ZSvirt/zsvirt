package org.zstack.sdk.identity.ldap.api;

import org.zstack.sdk.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory;

public class CreateLdapBindingResult {
    public AccountThirdPartyAccountSourceRefInventory inventory;
    public void setInventory(AccountThirdPartyAccountSourceRefInventory inventory) {
        this.inventory = inventory;
    }
    public AccountThirdPartyAccountSourceRefInventory getInventory() {
        return this.inventory;
    }

}
