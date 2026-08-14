package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIShareResourceToGroupEvent extends APIEvent {
    public APIShareResourceToGroupEvent(String apiId) {
        super(apiId);
    }

    public APIShareResourceToGroupEvent() {
        super(null);
    }

    public static APIShareResourceToGroupEvent __example__() {
        return new APIShareResourceToGroupEvent();
    }
}
