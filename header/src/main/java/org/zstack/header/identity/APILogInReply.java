package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * author:kaicai.hu
 * Date:2019/4/24
 */
@RestResponse(allTo = "inventory")
public class APILogInReply extends APIReply {
    private SessionInventory inventory;

    public SessionInventory getInventory() {
        return inventory;
    }

    public void setInventory(SessionInventory inventory) {
        this.inventory = inventory;
    }

    public static APILogInReply __example__() {
        APILogInReply reply = new APILogInReply();

        SessionInventory inventory = new SessionInventory();
        inventory.setUuid(uuid());
        inventory.setAccountUuid(uuid());
        inventory.setExpiredDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventory(inventory);
        return reply;
    }
}
