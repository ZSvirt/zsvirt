package org.zstack.allocator.domain.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteAllocatorDomainEvent extends APIEvent {
    public APIDeleteAllocatorDomainEvent() {
        super(null);
    }

    public APIDeleteAllocatorDomainEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteAllocatorDomainEvent __example__() {
        APIDeleteAllocatorDomainEvent event = new APIDeleteAllocatorDomainEvent();
        event.setSuccess(true);
        return event;
    }
}
