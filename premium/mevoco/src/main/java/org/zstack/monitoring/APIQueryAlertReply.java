package org.zstack.monitoring;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestResponse(allTo = "inventories")
public class APIQueryAlertReply extends APIQueryReply {
    private List<AlertInventory> inventories;

    public List<AlertInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<AlertInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryAlertReply __example__() {
        APIQueryAlertReply reply = new APIQueryAlertReply();
        AlertInventory inv = new AlertInventory();
        inv.setUuid(uuid());
        inv.setTriggerUuid(uuid());
        inv.setContent("alert content");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(inv));
        return reply;
    }
}
