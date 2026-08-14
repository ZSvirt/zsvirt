package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/10/18.
 */

@RestResponse
public class APIChangeVmPasswordEvent extends APIEvent {
    public APIChangeVmPasswordEvent() {
    }

    public APIChangeVmPasswordEvent(String apiId) {
        super(apiId);
    }

 
    public static APIChangeVmPasswordEvent __example__() {
        APIChangeVmPasswordEvent event = new APIChangeVmPasswordEvent();


        return event;
    }

}
