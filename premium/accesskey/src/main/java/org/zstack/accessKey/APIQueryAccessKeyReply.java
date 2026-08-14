package org.zstack.accessKey;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventories")
public class APIQueryAccessKeyReply extends APIQueryReply {
    private List<AccessKeyInventory> inventories;

    public List<AccessKeyInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AccessKeyInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryAccessKeyReply __example__() {
        APIQueryAccessKeyReply reply = new APIQueryAccessKeyReply();

        AccessKeyInventory inventory = new AccessKeyInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setUserUuid(inventory.getAccountUuid());
        inventory.setState(AccessKeyState.Enabled);
        inventory.setAccessKeyID("1234567890abcdedfhij");
        inventory.setAccessKeySecret("1234567890abcdedfhij1234567890abcdedfhij");

        reply.setInventories(list(inventory));
        return reply;
    }

}
