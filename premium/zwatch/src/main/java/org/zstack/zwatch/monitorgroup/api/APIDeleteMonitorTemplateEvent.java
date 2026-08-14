package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteMonitorTemplateEvent extends APIEvent {
    public APIDeleteMonitorTemplateEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteMonitorTemplateEvent() {
        super(null);
    }

    public static APIDeleteMonitorTemplateEvent __example__() {
        APIDeleteMonitorTemplateEvent evt = new APIDeleteMonitorTemplateEvent();
        evt.setSuccess(true);

        return evt;
    }
}
