package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobInventory;
import java.sql.Timestamp;


/**
 * Created by AlanJager on 2017/6/10.
 */
@RestResponse(allTo = "inventory")
public class APICreateSchedulerJobEvent extends APIEvent {
    SchedulerJobInventory inventory;

    public APICreateSchedulerJobEvent() {
    }

    public APICreateSchedulerJobEvent(String apiId) {
        super(apiId);
    }

    public SchedulerJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateSchedulerJobEvent __example__() {
        APICreateSchedulerJobEvent evt = new APICreateSchedulerJobEvent();
        SchedulerJobInventory scheduler = new SchedulerJobInventory();
        scheduler.setUuid(uuid());
        scheduler.setName("SchedulerJob");
        scheduler.setTargetResourceUuid(uuid());
        scheduler.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        scheduler.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setInventory(scheduler);

        return evt;
    }
}
