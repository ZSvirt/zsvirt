package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

@RestResponse(fieldsTo = {"inventory"})
public class APIConvertVmInstanceToTemplatedVmInstanceEvent extends APIEvent {
    TemplatedVmInstanceInventory inventory;

    public APIConvertVmInstanceToTemplatedVmInstanceEvent() {
        super(null);
    }

    public APIConvertVmInstanceToTemplatedVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public TemplatedVmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(TemplatedVmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    public static APIConvertVmInstanceToTemplatedVmInstanceEvent __example__() {
        APIConvertVmInstanceToTemplatedVmInstanceEvent event = new APIConvertVmInstanceToTemplatedVmInstanceEvent();

        TemplatedVmInstanceInventory inventory = new TemplatedVmInstanceInventory();
        inventory.setUuid(uuid());
        inventory.setName("templatedVmInstance");
        inventory.setZoneUuid(uuid());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        event.setInventory(inventory);
        return event;
    }
}
