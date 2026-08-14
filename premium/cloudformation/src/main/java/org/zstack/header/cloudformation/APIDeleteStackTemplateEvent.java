package org.zstack.header.cloudformation;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@RestResponse
public class APIDeleteStackTemplateEvent extends APIEvent {
    public APIDeleteStackTemplateEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteStackTemplateEvent() {
        super(null);
    }

    public static APIDeleteStackTemplateEvent __example__() {
        APIDeleteStackTemplateEvent event = new APIDeleteStackTemplateEvent();
        return event;
    }
}
