package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachRoleFromAccountEvent extends APIEvent {
    public APIDetachRoleFromAccountEvent() {
    }

    public APIDetachRoleFromAccountEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachRoleFromAccountEvent __example__() {
        APIDetachRoleFromAccountEvent event = new APIDetachRoleFromAccountEvent();

        return event;
    }
}
