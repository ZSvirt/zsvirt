package org.zstack.templateConfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRevertTemplateConfigEvent extends APIEvent {
    public APIRevertTemplateConfigEvent() {
        super(null);
    }

    public APIRevertTemplateConfigEvent(String apiId) {
        super(apiId);
    }

    public static APIRevertTemplateConfigEvent __example__() {
        APIRevertTemplateConfigEvent event = new APIRevertTemplateConfigEvent();
        event.setSuccess(true);
        return event;
    }
}
