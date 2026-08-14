package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobSchedulerTriggerInventory;
import org.zstack.header.scheduler.SchedulerJobSchedulerTriggerRefVO;

/**
 * Created by AlanJager on 2017/6/8.
 */
@RestResponse(allTo = "inventory")
public class APIAddSchedulerJobToSchedulerTriggerEvent extends APIEvent{
    SchedulerJobSchedulerTriggerInventory inventory;

    public APIAddSchedulerJobToSchedulerTriggerEvent() {
        super(null);
    }

    public APIAddSchedulerJobToSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public SchedulerJobSchedulerTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerJobSchedulerTriggerInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddSchedulerJobToSchedulerTriggerEvent __example__() {
        APIAddSchedulerJobToSchedulerTriggerEvent evt = new APIAddSchedulerJobToSchedulerTriggerEvent();
        SchedulerJobSchedulerTriggerRefVO vo = new SchedulerJobSchedulerTriggerRefVO();
        vo.setUuid(uuid());
        vo.setSchedulerJobUuid(uuid());
        vo.setSchedulerTriggerUuid(uuid());

        SchedulerJobSchedulerTriggerInventory inv = SchedulerJobSchedulerTriggerInventory.valueOf(vo);
        evt.setInventory(inv);
        return evt;
    }
}
