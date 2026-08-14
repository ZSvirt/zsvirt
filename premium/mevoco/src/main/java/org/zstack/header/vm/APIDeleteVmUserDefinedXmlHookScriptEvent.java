package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVmUserDefinedXmlHookScriptEvent extends APIEvent {
    public APIDeleteVmUserDefinedXmlHookScriptEvent() {
        super(null);
    }

    public APIDeleteVmUserDefinedXmlHookScriptEvent(String apiId) {
        super(apiId);
    }

 
    public static APIDeleteVmUserDefinedXmlHookScriptEvent __example__() {
        APIDeleteVmUserDefinedXmlHookScriptEvent event = new APIDeleteVmUserDefinedXmlHookScriptEvent();

        return event;
    }

}
