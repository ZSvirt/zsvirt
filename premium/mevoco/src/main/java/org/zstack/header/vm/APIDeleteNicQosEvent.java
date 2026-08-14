package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestResponse
public class APIDeleteNicQosEvent extends APIEvent {
    public APIDeleteNicQosEvent() {
        super(null);
    }

    public APIDeleteNicQosEvent(String apiId) {
        super(apiId);
    }

 
    public static APIDeleteNicQosEvent __example__() {
        APIDeleteNicQosEvent event = new APIDeleteNicQosEvent();

        return event;
    }

}
