package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUnsubscribeSNSTopicEvent extends APIEvent {
    public static APIUnsubscribeSNSTopicEvent __example__() {
        return new APIUnsubscribeSNSTopicEvent();
    }

    public APIUnsubscribeSNSTopicEvent() {
    }

    public APIUnsubscribeSNSTopicEvent(String apiId) {
        super(apiId);
    }
}
