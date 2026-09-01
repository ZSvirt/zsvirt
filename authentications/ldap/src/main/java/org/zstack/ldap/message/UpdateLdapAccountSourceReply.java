package org.zstack.ldap.message;

import org.zstack.header.message.MessageReply;
import org.zstack.ldap.entity.LdapServerInventory;

public class UpdateLdapAccountSourceReply extends MessageReply {
    private LdapServerInventory inventory;

    public LdapServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LdapServerInventory inventory) {
        this.inventory = inventory;
    }
}