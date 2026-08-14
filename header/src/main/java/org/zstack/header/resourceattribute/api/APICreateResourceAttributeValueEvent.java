package org.zstack.header.resourceattribute.api;

import org.zstack.header.resourceattribute.entity.CreateResourceAttributeResult;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestResponse(allTo = "inventories")
public class APICreateResourceAttributeValueEvent extends APIEvent {
    private List<CreateResourceAttributeResult> inventories;

    public APICreateResourceAttributeValueEvent() {
    }

    public APICreateResourceAttributeValueEvent(String apiId) {
        super(apiId);
    }

    public List<CreateResourceAttributeResult> getInventories() {
        return inventories;
    }

    public void setInventories(List<CreateResourceAttributeResult> inventories) {
        this.inventories = inventories;
    }

    public static APICreateResourceAttributeValueEvent __example__() {
        APICreateResourceAttributeValueEvent event = new APICreateResourceAttributeValueEvent();
        event.setInventories(list(CreateResourceAttributeResult.__example__()));
        return event;
    }
}
