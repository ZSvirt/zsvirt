package org.zstack.sdk.identity.ldap.api;

import org.zstack.sdk.identity.ldap.entity.LdapServerInventory;

public class UpdateLdapServerResult {
    public LdapServerInventory inventory;
    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
    public LdapServerInventory getInventory() {
        return this.inventory;
    }

}
