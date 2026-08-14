package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobGroupInventory;
import org.zstack.header.scheduler.SchedulerJobInventory;

import java.sql.Timestamp;

@RestResponse(allTo = "inventory")
public class APIUpdateSchedulerJobGroupEvent extends APIEvent {
    private SchedulerJobGroupInventory inventory;

    public APIUpdateSchedulerJobGroupEvent(String apiId) {
        super(apiId);
    }

    public APIUpdateSchedulerJobGroupEvent() {
        super(null);
    }

    public SchedulerJobGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobGroupInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSchedulerJobGroupEvent __example__() {
        APIUpdateSchedulerJobGroupEvent event = new APIUpdateSchedulerJobGroupEvent();
        SchedulerJobGroupInventory scheduler = new SchedulerJobGroupInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("Test");
        scheduler.setDescription("create volume snapshot job");
        scheduler.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        scheduler.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setSuccess(true);
        event.setInventory(scheduler);
        return event;
    }

}
