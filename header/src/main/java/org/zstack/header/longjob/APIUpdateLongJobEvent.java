package org.zstack.header.longjob;

import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *  * Created on 2/27/2020
 *   */

@RestResponse(allTo = "inventory")
public class APIUpdateLongJobEvent extends APIEvent {
    private LongJobInventory inventory;

    public APIUpdateLongJobEvent() {

    }

    public APIUpdateLongJobEvent(String apiId) {
        super(apiId);
    }

    public LongJobInventory getInventory() {
        return inventory;
    }

    public void setInventory(LongJobInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateLongJobEvent __example__() {
        APIUpdateLongJobEvent event = new APIUpdateLongJobEvent();
        LongJobInventory lj = new LongJobInventory();
        lj.setUuid(uuid());
        lj.setName("new-name");
        lj.setDescription("new-description");
        event.setInventory(lj);
        return event;
    }

}

