package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteSNSTopicEvent extends APIEvent {
    public static APIDeleteSNSTopicEvent __example__() {
        return new APIDeleteSNSTopicEvent();
    }

    public APIDeleteSNSTopicEvent() {
    }

    public APIDeleteSNSTopicEvent(String apiId) {
        super(apiId);
    }
}
