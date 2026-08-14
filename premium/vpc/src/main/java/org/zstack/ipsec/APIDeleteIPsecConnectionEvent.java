package org.zstack.ipsec;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/11/3.
 */
@RestResponse
public class APIDeleteIPsecConnectionEvent extends APIEvent {
    public APIDeleteIPsecConnectionEvent() {
    }

    public APIDeleteIPsecConnectionEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteIPsecConnectionEvent __example__() {
        APIDeleteIPsecConnectionEvent event = new APIDeleteIPsecConnectionEvent();

        event.setSuccess(true);

        return event;
    }

}
