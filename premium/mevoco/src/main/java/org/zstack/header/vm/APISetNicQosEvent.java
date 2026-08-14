package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestResponse
public class APISetNicQosEvent extends APIEvent {
    public APISetNicQosEvent() {
    }

    public APISetNicQosEvent(String apiId) {
        super(apiId);
    }

 
    public static APISetNicQosEvent __example__() {
        APISetNicQosEvent event = new APISetNicQosEvent();

        return event;
    }

}
