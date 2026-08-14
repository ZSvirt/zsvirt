package org.zstack.scheduler;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 7/18/16.
 */
@RestResponse(allTo = "inventories")
public class APIQuerySchedulerJobReply extends APIQueryReply {
    private List<SchedulerJobInventory> inventories;

    public List<SchedulerJobInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerJobInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIQuerySchedulerJobReply __example__() {
        APIQuerySchedulerJobReply reply = new APIQuerySchedulerJobReply();
        SchedulerJobInventory scheduler = new SchedulerJobInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("test");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        scheduler.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(scheduler));
        reply.setSuccess(true);
        return reply;
    }

}
