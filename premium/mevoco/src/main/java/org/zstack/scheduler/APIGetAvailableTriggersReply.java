package org.zstack.scheduler;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerTriggerInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by kayo on 2017/9/8.
 */
@RestResponse(allTo = "inventories")
public class APIGetAvailableTriggersReply extends APIReply {
    private List<SchedulerTriggerInventory> inventories;

    public List<SchedulerTriggerInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerTriggerInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetAvailableTriggersReply __example__() {
        APIGetAvailableTriggersReply reply = new APIGetAvailableTriggersReply();

        SchedulerTriggerInventory inv = new SchedulerTriggerInventory();
        inv.setName("trigger");
        inv.setDescription("this is a scheduler trigger");
        inv.setUuid(uuid());
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inv));

        return reply;
    }
}
