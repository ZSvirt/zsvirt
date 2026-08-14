package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveActionFromEventSubscriptionEvent extends APIEvent {

    public APIRemoveActionFromEventSubscriptionEvent() {
    }

    public APIRemoveActionFromEventSubscriptionEvent(String apiId) {
        super(apiId);
    }
}
