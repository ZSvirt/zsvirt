package org.zstack.scheduler;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerTriggerInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;


/**
 * Created by AlanJager on 2017/6/8.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySchedulerTriggerReply  extends APIQueryReply {
    private List<SchedulerTriggerInventory> inventories;

    public List<SchedulerTriggerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerTriggerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySchedulerTriggerReply __example__() {
        APIQuerySchedulerTriggerReply reply = new APIQuerySchedulerTriggerReply();
        SchedulerTriggerInventory inv = new SchedulerTriggerInventory();
        inv.setUuid(uuid());
        inv.setName("test");
        inv.setDescription("a test trigger");
        inv.setStartTime(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setStopTime(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(inv));
        return reply;
    }
}
