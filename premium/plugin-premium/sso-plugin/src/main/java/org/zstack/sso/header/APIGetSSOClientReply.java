package org.zstack.sso.header;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@RestResponse(fieldsTo = "inventories")
public class APIGetSSOClientReply extends APIReply {
    private List<ThirdPartyAccountSourceInventory> inventories;

    public List<ThirdPartyAccountSourceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ThirdPartyAccountSourceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetSSOClientReply __example__() {
        APIGetSSOClientReply reply = new APIGetSSOClientReply();
        ThirdPartyAccountSourceInventory inv = ThirdPartyAccountSourceInventory.__example__();
        inv.setType("OAuth2");
        reply.setInventories(new ArrayList<>());
        reply.getInventories().add(inv);
        return reply;
    }
}
