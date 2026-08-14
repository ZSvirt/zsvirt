package org.zstack.header.managementnode;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUpdateFactoryModeStateEvent extends APIEvent {
    public APIUpdateFactoryModeStateEvent() {
    }

    public APIUpdateFactoryModeStateEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateFactoryModeStateEvent __example__() {
        APIUpdateFactoryModeStateEvent evt = new APIUpdateFactoryModeStateEvent();
        return evt;
    }
}
