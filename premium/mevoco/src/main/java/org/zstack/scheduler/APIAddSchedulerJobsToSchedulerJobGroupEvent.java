package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobGroupJobRefInventory;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIAddSchedulerJobsToSchedulerJobGroupEvent extends APIEvent {
    private List<SchedulerJobGroupJobRefInventory> inventories;
    public APIAddSchedulerJobsToSchedulerJobGroupEvent() {
        super(null);
    }

    public APIAddSchedulerJobsToSchedulerJobGroupEvent(String apiId) {
        super(apiId);
    }

    public List<SchedulerJobGroupJobRefInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerJobGroupJobRefInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIAddSchedulerJobsToSchedulerJobGroupEvent __example__() {
        APIAddSchedulerJobsToSchedulerJobGroupEvent evt = new APIAddSchedulerJobsToSchedulerJobGroupEvent();
        return evt;
    }
}
