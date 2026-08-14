package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by yaohua.wu on 18/9/2019.
 */
@RestResponse
public class APIUpdatePriorityConfigEvent extends APIEvent {

    public APIUpdatePriorityConfigEvent() {
    }

    public APIUpdatePriorityConfigEvent(String apiId) {
        super(apiId);
    }

 
    public static APIUpdatePriorityConfigEvent __example__() {
        APIUpdatePriorityConfigEvent event = new APIUpdatePriorityConfigEvent();


        return event;
    }

}
