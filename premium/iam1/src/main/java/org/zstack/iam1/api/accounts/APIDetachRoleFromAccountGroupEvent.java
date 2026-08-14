package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachRoleFromAccountGroupEvent extends APIEvent {
    public APIDetachRoleFromAccountGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDetachRoleFromAccountGroupEvent() {
        super(null);
    }

    public static APIDetachRoleFromAccountGroupEvent __example__() {
        return new APIDetachRoleFromAccountGroupEvent();
    }
}
