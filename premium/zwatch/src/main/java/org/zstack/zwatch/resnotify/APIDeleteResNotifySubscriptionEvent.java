package org.zstack.zwatch.resnotify;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteResNotifySubscriptionEvent extends APIEvent {
    public APIDeleteResNotifySubscriptionEvent() {
    }

    public APIDeleteResNotifySubscriptionEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteResNotifySubscriptionEvent __example__() {
        return new APIDeleteResNotifySubscriptionEvent();
    }
}
