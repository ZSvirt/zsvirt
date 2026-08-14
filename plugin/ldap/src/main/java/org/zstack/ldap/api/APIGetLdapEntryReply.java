package org.zstack.ldap.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.ldap.entity.LdapEntryInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2017/11/03.
 */
@RestResponse(allTo = "inventories")
public class APIGetLdapEntryReply extends APIReply {
    private List<LdapEntryInventory> inventories;

    public List<LdapEntryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<LdapEntryInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetLdapEntryReply __example__() {
        APIGetLdapEntryReply reply = new APIGetLdapEntryReply();
        reply.setInventories(new ArrayList<>());
        return reply;
    }

}
