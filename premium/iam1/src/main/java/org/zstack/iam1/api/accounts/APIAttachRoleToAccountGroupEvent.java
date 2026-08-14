package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachRoleToAccountGroupEvent extends APIEvent {
    public APIAttachRoleToAccountGroupEvent(String apiId) {
        super(apiId);
    }

    public APIAttachRoleToAccountGroupEvent() {
        super(null);
    }

    public static APIAttachRoleToAccountGroupEvent __example__() {
        return new APIAttachRoleToAccountGroupEvent();
    }
}
