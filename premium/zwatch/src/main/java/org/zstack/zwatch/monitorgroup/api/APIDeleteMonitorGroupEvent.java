package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteMonitorGroupEvent extends APIEvent {
    public APIDeleteMonitorGroupEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteMonitorGroupEvent() {
        super(null);
    }

    public static APIDeleteMonitorGroupEvent __example__() {
        APIDeleteMonitorGroupEvent evt = new APIDeleteMonitorGroupEvent();
        evt.setSuccess(true);

        return evt;
    }
}
