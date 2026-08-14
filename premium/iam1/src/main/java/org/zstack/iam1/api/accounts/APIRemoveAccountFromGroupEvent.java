package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveAccountFromGroupEvent extends APIEvent {
    public APIRemoveAccountFromGroupEvent(String apiId) {
        super(apiId);
    }

    public APIRemoveAccountFromGroupEvent() {
        super(null);
    }

    public static APIRemoveAccountFromGroupEvent __example__() {
        return new APIRemoveAccountFromGroupEvent();
    }
}
