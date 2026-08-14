package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APICreateTemplatedVmInstanceFromVmInstanceEvent extends APIEvent {
    private TemplatedVmInstanceInventory templatedVmInstanceInventory;

    public APICreateTemplatedVmInstanceFromVmInstanceEvent() {
        super(null);
    }

    public APICreateTemplatedVmInstanceFromVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public TemplatedVmInstanceInventory getTemplatedVmInstanceInventory() {
        return templatedVmInstanceInventory;
    }

    public void setTemplatedVmInstanceInventory(TemplatedVmInstanceInventory templatedVmInstanceInventory) {
        this.templatedVmInstanceInventory = templatedVmInstanceInventory;
    }

    public static APICreateTemplatedVmInstanceFromVmInstanceEvent __example__() {
        APICreateTemplatedVmInstanceFromVmInstanceEvent event = new APICreateTemplatedVmInstanceFromVmInstanceEvent();

        TemplatedVmInstanceInventory inv = new TemplatedVmInstanceInventory();
        inv.setUuid(uuid());
        inv.setName("templated vmInstance");

        event.setTemplatedVmInstanceInventory(inv);
        return event;
    }
}
