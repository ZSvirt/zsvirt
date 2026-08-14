package org.zstack.zwatch.alarm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUnsubscribeEventEvent extends APIEvent {
    public APIUnsubscribeEventEvent() {
    }

    public APIUnsubscribeEventEvent(String apiId) {
        super(apiId);
    }
}
