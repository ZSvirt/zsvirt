package org.zstack.templateConfig;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIApplyTemplateConfigEvent  extends APIEvent {
    public APIApplyTemplateConfigEvent(String apiId) {
        super(apiId);
    }
    public APIApplyTemplateConfigEvent() {
        super(null);
    }

    public static APIApplyTemplateConfigEvent __example__() {
        APIApplyTemplateConfigEvent event = new APIApplyTemplateConfigEvent();
        event.setSuccess(true);
        return event;
    }

}

