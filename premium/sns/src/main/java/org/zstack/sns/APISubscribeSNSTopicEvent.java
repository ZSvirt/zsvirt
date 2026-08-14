package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISubscribeSNSTopicEvent extends APIEvent {
    public static APISubscribeSNSTopicEvent __example__() {
        return new APISubscribeSNSTopicEvent();
    }

    public APISubscribeSNSTopicEvent() {
    }

    public APISubscribeSNSTopicEvent(String apiId) {
        super(apiId);
    }
}
