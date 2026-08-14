package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobGroupSchedulerTriggerRefInventory;
import org.zstack.header.scheduler.SchedulerJobGroupSchedulerTriggerRefVO;

@RestResponse(allTo = "inventory")
public class APIAddSchedulerJobGroupToSchedulerTriggerEvent extends APIEvent{
    SchedulerJobGroupSchedulerTriggerRefInventory inventory;

    public APIAddSchedulerJobGroupToSchedulerTriggerEvent() {
        super(null);
    }

    public APIAddSchedulerJobGroupToSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public SchedulerJobGroupSchedulerTriggerRefInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobGroupSchedulerTriggerRefInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddSchedulerJobGroupToSchedulerTriggerEvent __example__() {
        APIAddSchedulerJobGroupToSchedulerTriggerEvent evt = new APIAddSchedulerJobGroupToSchedulerTriggerEvent();
        SchedulerJobGroupSchedulerTriggerRefVO vo = new SchedulerJobGroupSchedulerTriggerRefVO();
        vo.setSchedulerJobGroupUuid(uuid());
        vo.setSchedulerTriggerUuid(uuid());

        SchedulerJobGroupSchedulerTriggerRefInventory inv = SchedulerJobGroupSchedulerTriggerRefInventory.valueOf(vo);
        evt.setInventory(inv);
        return evt;
    }
}
