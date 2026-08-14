package org.zstack.header.core.external.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIReloadExternalServiceEvent extends APIEvent {
    public APIReloadExternalServiceEvent() { }

    public APIReloadExternalServiceEvent(String apiId) {
        super(apiId);
    }

    public static APIReloadExternalServiceEvent __example__() {
        return new APIReloadExternalServiceEvent();
    }
}
