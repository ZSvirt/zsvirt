package org.zstack.core.gc;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/3/5.
 */
@RestResponse
public class APIDeleteGCJobEvent extends APIEvent {
    public APIDeleteGCJobEvent() {
    }

    public APIDeleteGCJobEvent(String apiId) {
        super(apiId);
    }
    public static APIDeleteGCJobEvent __example__() {
        APIDeleteGCJobEvent event = new APIDeleteGCJobEvent();
        event.setSuccess(true);
        return event;
    }
}
