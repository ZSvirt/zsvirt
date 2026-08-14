package org.zstack.scheduler;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobInventory;
import org.zstack.header.scheduler.SchedulerJobVO;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by kayo on 2018/4/13.
 */
@RestResponse(allTo = "inventories")
public class APIGetNoTriggerSchedulerJobsReply extends APIReply {
    private List<SchedulerJobInventory> inventories;

    public List<SchedulerJobInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerJobInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetNoTriggerSchedulerJobsReply __example__() {
        APIGetNoTriggerSchedulerJobsReply reply = new APIGetNoTriggerSchedulerJobsReply();

        SchedulerJobInventory inv = new SchedulerJobInventory();
        inv.setName("job");
        inv.setDescription("this is a scheduler job");
        inv.setUuid(uuid(SchedulerJobVO.class));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(inv));

        return reply;
    }
}
