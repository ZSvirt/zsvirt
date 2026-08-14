package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteTemplatedVmInstanceEvent extends APIEvent {
    public APIDeleteTemplatedVmInstanceEvent() {
        super(null);
    }

    public APIDeleteTemplatedVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteTemplatedVmInstanceEvent __example__() {
        APIDeleteTemplatedVmInstanceEvent event = new APIDeleteTemplatedVmInstanceEvent();
        event.setSuccess(true);
        return event;
    }
}
