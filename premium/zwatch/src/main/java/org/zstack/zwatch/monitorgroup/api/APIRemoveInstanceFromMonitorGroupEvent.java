package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveInstanceFromMonitorGroupEvent extends APIEvent {
    public APIRemoveInstanceFromMonitorGroupEvent(String apiId) {
        super(apiId);
    }

    public APIRemoveInstanceFromMonitorGroupEvent() {
        super(null);
    }

    public static APIRemoveInstanceFromMonitorGroupEvent __example__() {
        APIRemoveInstanceFromMonitorGroupEvent evt = new APIRemoveInstanceFromMonitorGroupEvent();
        evt.setSuccess(true);

        return evt;
    }
}
