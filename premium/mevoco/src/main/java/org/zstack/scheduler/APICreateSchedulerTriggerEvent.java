package org.zstack.scheduler;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerTriggerInventory;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */
@RestResponse(allTo = "inventory")
public class APICreateSchedulerTriggerEvent extends APIEvent {
    private SchedulerTriggerInventory inventory;

    public APICreateSchedulerTriggerEvent() {
        super(null);
    }

    public SchedulerTriggerInventory getInventory() {
        return inventory;
    }

    public void setInventory(SchedulerTriggerInventory inventory) {
        this.inventory = inventory;
    }

    public APICreateSchedulerTriggerEvent(String apiId) {
        super(apiId);
    }

    public static APICreateSchedulerTriggerEvent __example__() {
        APICreateSchedulerTriggerEvent evt = new APICreateSchedulerTriggerEvent();
        SchedulerTriggerInventory inv = new SchedulerTriggerInventory();
        inv.setName("trigger");
        inv.setDescription("this is a scheduler trigger");
        inv.setUuid(uuid());
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        evt.setInventory(inv);

        return evt;
    }
}
