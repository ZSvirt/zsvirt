package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteAccountGroupEvent extends APIEvent {
    public APIDeleteAccountGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAccountGroupEvent() {
        super(null);
    }

    public static APIDeleteAccountGroupEvent __example__() {
        return new APIDeleteAccountGroupEvent();
    }
}
