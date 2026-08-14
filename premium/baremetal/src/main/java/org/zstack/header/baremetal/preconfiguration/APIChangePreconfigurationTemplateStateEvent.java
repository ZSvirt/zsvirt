package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2018-12-26.
 */
@RestResponse(allTo = "inventory")
public class APIChangePreconfigurationTemplateStateEvent extends APIEvent {
    private PreconfigurationTemplateInventory inventory;

    public PreconfigurationTemplateInventory getInventory() {
        return inventory;
    }

    public void setInventory(PreconfigurationTemplateInventory inventory) {
        this.inventory = inventory;
    }

    public APIChangePreconfigurationTemplateStateEvent() {
    }

    public APIChangePreconfigurationTemplateStateEvent(String apiId) {
        super(apiId);
    }

    public static APIChangePreconfigurationTemplateStateEvent __example__() {
        APIChangePreconfigurationTemplateStateEvent event = new APIChangePreconfigurationTemplateStateEvent();
        event.setInventory(PreconfigurationTemplateInventory.__example__());
        return event;
    }
}
