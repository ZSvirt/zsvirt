package org.zstack.header.protocol;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVRouterOspfAreaReply extends APIQueryReply {
    private List<RouterAreaInventory> inventories;

    public List<RouterAreaInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<RouterAreaInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVRouterOspfAreaReply __example__() {
        APIQueryVRouterOspfAreaReply reply = new APIQueryVRouterOspfAreaReply();
        RouterAreaInventory area = new RouterAreaInventory();
        area.setUuid(uuid());
        area.setType("Standard");
        area.setAreaId("1");
        area.setAuthentication("None");
        area.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        area.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setSuccess(true);
        reply.setInventories(asList(area));
        return reply;
    }
}
