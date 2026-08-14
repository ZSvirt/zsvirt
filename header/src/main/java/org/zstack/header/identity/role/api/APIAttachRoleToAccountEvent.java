package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachRoleToAccountEvent extends APIEvent {
    public APIAttachRoleToAccountEvent() {
    }

    public APIAttachRoleToAccountEvent(String apiId) {
        super(apiId);
    }

    public static APIAttachRoleToAccountEvent __example__() {
        APIAttachRoleToAccountEvent event = new APIAttachRoleToAccountEvent();

        return event;
    }
}
