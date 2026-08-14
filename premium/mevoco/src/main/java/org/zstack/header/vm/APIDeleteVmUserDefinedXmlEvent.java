package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVmUserDefinedXmlEvent extends APIEvent {
    public APIDeleteVmUserDefinedXmlEvent() {
        super(null);
    }

    public APIDeleteVmUserDefinedXmlEvent(String apiId) {
        super(apiId);
    }

 
    public static APIDeleteVmUserDefinedXmlEvent __example__() {
        APIDeleteVmUserDefinedXmlEvent event = new APIDeleteVmUserDefinedXmlEvent();

        return event;
    }

}
