package org.zstack.header.tag;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryUserTagReply extends APIQueryReply {
    private List<UserTagInventory> inventories;

    public List<UserTagInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UserTagInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQueryUserTagReply __example__() {
        APIQueryUserTagReply reply = new APIQueryUserTagReply();
        UserTagInventory tag = new UserTagInventory();
        tag.setType("User");
        tag.setResourceType(uuid());
        tag.setResourceType("DiskOfferingVO");
        tag.setTag("for-large-DB");
        tag.setUuid(uuid()  );
        tag.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        tag.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setSuccess(true);
        reply.setInventories(asList(tag));
        return reply;
    }

}
