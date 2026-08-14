package org.zstack.header.cluster;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 3/12/18
 */
@RestResponse(allTo = "inventory")
public class APIUpdateClusterOSEvent extends APIEvent {
    private LongJobInventory inventory;

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateClusterOSEvent() {
    }

    public APIUpdateClusterOSEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateClusterOSEvent __example__() {
        APIUpdateClusterOSEvent event = new APIUpdateClusterOSEvent();
        LongJobInventory inv = new LongJobInventory();
        inv.setUuid(uuid());
        event.setInventory(inv);
        return event;
    }
}
