package org.zstack.header.cloudformation;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/6/11.
 */
@RestResponse
public class APIDeleteResourceStackEvent extends APIEvent {
    public APIDeleteResourceStackEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteResourceStackEvent() {
        super(null);
    }

    public static APIDeleteResourceStackEvent __example__() {
        APIDeleteResourceStackEvent event = new APIDeleteResourceStackEvent();
        return event;
    }
}
