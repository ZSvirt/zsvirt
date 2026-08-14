package org.zstack.loginControl.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteAccessControlRuleEvent extends APIEvent {
    public APIDeleteAccessControlRuleEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteAccessControlRuleEvent() {
        super(null);
    }
}
