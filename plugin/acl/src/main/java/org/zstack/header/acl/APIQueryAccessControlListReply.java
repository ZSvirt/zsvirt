package org.zstack.header.acl;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@RestResponse(allTo = "inventories")
public class APIQueryAccessControlListReply extends APIQueryReply {
    private List<AccessControlListInventory> inventories;

    public List<AccessControlListInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccessControlListInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAccessControlListReply __example__() {
        APIQueryAccessControlListReply reply = new APIQueryAccessControlListReply();
        AccessControlListInventory inv = new AccessControlListInventory();

        inv.setName("acl-group");
        inv.setUuid(uuid());

        reply.setInventories(Arrays.asList(inv));
        return reply;
    }

}