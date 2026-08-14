package org.zstack.templateConfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIResetTemplateConfigEvent extends APIEvent {

    public APIResetTemplateConfigEvent(String apiId) {
        super(apiId);
    }
    public APIResetTemplateConfigEvent() {
        super(null);
    }

    public static APIResetTemplateConfigEvent __example__() {
        APIResetTemplateConfigEvent event = new APIResetTemplateConfigEvent();
        event.setSuccess(true);
        return event;
    }

}
