package org.zstack.ldap.api;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.ldap.entity.LdapServerInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APIQueryLdapServerReply extends APIQueryReply {
    private List<LdapServerInventory> inventories;

    public List<LdapServerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LdapServerInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryLdapServerReply __example__() {
        APIQueryLdapServerReply reply = new APIQueryLdapServerReply();
        LdapServerInventory inventory = new LdapServerInventory();
        inventory.setUuid(uuid());
        inventory.setName("miao");
        inventory.setDescription("miao desc");
        inventory.setUrl("ldap://localhost:1888");
        inventory.setBase("dc=example,dc=com");
        inventory.setUsername("");
        inventory.setEncryption("None");

        reply.setInventories(list(inventory));
        return reply;
    }

}
