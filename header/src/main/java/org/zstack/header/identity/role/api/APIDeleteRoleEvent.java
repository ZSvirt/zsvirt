package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteRoleEvent extends APIEvent {
    public APIDeleteRoleEvent() {
    }

    public APIDeleteRoleEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteRoleEvent __example__() {
        APIDeleteRoleEvent event = new APIDeleteRoleEvent();
        return event;
    }
}
