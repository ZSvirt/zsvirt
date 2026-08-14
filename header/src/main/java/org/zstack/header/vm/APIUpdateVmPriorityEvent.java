package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by yaohua.wu on 18/9/2019.
 */
@RestResponse
public class APIUpdateVmPriorityEvent extends APIEvent {

    public APIUpdateVmPriorityEvent() {
    }

    public APIUpdateVmPriorityEvent(String apiId) {
        super(apiId);
    }

 
    public static APIUpdateVmPriorityEvent __example__() {
        APIUpdateVmPriorityEvent event = new APIUpdateVmPriorityEvent();


        return event;
    }

}
