package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobGroupInventory;

import java.sql.Timestamp;


@RestResponse(allTo = "inventory")
public class APICreateSchedulerJobGroupEvent extends APIEvent {
    SchedulerJobGroupInventory inventory;

    public APICreateSchedulerJobGroupEvent() {
    }

    public APICreateSchedulerJobGroupEvent(String apiId) {
        super(apiId);
    }

    public SchedulerJobGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateSchedulerJobGroupEvent __example__() {
        APICreateSchedulerJobGroupEvent evt = new APICreateSchedulerJobGroupEvent();
        SchedulerJobGroupInventory jobGroup = new SchedulerJobGroupInventory();
        jobGroup.setUuid(uuid());
        jobGroup.setName("SchedulerJob");
        jobGroup.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        jobGroup.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setInventory(jobGroup);

        return evt;
    }
}
