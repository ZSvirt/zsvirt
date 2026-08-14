package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveLabelFromEventSubscriptionEvent extends APIEvent {
    public APIRemoveLabelFromEventSubscriptionEvent() {
    }

    public APIRemoveLabelFromEventSubscriptionEvent(String apiId) {
        super(apiId);
    }
}
