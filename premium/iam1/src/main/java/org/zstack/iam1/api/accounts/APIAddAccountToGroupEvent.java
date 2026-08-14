package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAddAccountToGroupEvent extends APIEvent {
    public APIAddAccountToGroupEvent(String apiId) {
        super(apiId);
    }

    public APIAddAccountToGroupEvent() {
        super(null);
    }

    public static APIAddAccountToGroupEvent __example__() {
        return new APIAddAccountToGroupEvent();
    }
}
