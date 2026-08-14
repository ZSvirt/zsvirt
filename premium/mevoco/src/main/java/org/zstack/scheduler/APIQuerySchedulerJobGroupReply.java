package org.zstack.scheduler;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobGroupInventory;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuerySchedulerJobGroupReply extends APIQueryReply {
    private List<SchedulerJobGroupInventory> inventories;

    public List<SchedulerJobGroupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerJobGroupInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySchedulerJobGroupReply __example__() {
        APIQuerySchedulerJobGroupReply reply = new APIQuerySchedulerJobGroupReply();
        SchedulerJobGroupInventory scheduler = new SchedulerJobGroupInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("test");
        scheduler.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        scheduler.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(Collections.singletonList(scheduler));
        reply.setSuccess(true);
        return reply;
    }

}
