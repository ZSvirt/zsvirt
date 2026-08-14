package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteEventRuleTemplateEvent extends APIEvent {
    public APIDeleteEventRuleTemplateEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteEventRuleTemplateEvent() {
        super(null);
    }

    public static APIDeleteEventRuleTemplateEvent __example__() {
        APIDeleteEventRuleTemplateEvent evt = new APIDeleteEventRuleTemplateEvent();
        evt.setSuccess(true);

        return evt;
    }
}
