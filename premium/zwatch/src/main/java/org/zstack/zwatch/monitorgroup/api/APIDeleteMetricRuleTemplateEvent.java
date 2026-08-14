package org.zstack.zwatch.monitorgroup.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteMetricRuleTemplateEvent extends APIEvent {
    public APIDeleteMetricRuleTemplateEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteMetricRuleTemplateEvent() {
        super(null);
    }

    public static APIDeleteMetricRuleTemplateEvent __example__() {
        APIDeleteMetricRuleTemplateEvent evt = new APIDeleteMetricRuleTemplateEvent();
        evt.setSuccess(true);

        return evt;
    }
}
