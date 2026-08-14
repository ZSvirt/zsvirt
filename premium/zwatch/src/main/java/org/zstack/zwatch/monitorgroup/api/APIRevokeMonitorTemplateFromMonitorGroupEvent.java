package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRevokeMonitorTemplateFromMonitorGroupEvent extends APIEvent {
    public APIRevokeMonitorTemplateFromMonitorGroupEvent(String apiId) {
        super(apiId);
    }

    public APIRevokeMonitorTemplateFromMonitorGroupEvent() {
        super(null);
    }

    public static APIRevokeMonitorTemplateFromMonitorGroupEvent __example__() {
        APIRevokeMonitorTemplateFromMonitorGroupEvent evt = new APIRevokeMonitorTemplateFromMonitorGroupEvent();
        evt.setSuccess(true);

        return evt;
    }
}
