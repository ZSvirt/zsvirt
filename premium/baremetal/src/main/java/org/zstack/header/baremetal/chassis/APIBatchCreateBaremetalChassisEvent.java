package org.zstack.header.baremetal.chassis;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2018-10-08.
 */
@RestResponse(fieldsTo = {"inventory"})
public class APIBatchCreateBaremetalChassisEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APIBatchCreateBaremetalChassisEvent() {
    }

    public APIBatchCreateBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIBatchCreateBaremetalChassisEvent __example__() {
        APIBatchCreateBaremetalChassisEvent evt = new APIBatchCreateBaremetalChassisEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        inv.setApiId(uuid());
        inv.setName("APIBatchCreateBaremetalChassisMsg");
        inv.setJobName("APIBatchCreateBaremetalChassisMsg");
        inv.setJobData("{\"createMessages\":[]");
        inv.setJobResult("");
        inv.setManagementNodeUuid(uuid());
        inv.setState(LongJobState.Succeeded);
        evt.setInventory(inv);
        return evt;
    }
}
