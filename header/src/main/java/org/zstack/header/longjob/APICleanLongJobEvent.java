package org.zstack.header.longjob;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by wushan on 8/23/21
 **/
@RestResponse
public class APICleanLongJobEvent extends APIEvent {
    public APICleanLongJobEvent() {
    }

    public APICleanLongJobEvent(String apiId) {
        super(apiId);
    }

    public static APICleanLongJobEvent __example__() {
        APICleanLongJobEvent event = new APICleanLongJobEvent();
        event.setSuccess(true);
        return event;
    }
}
