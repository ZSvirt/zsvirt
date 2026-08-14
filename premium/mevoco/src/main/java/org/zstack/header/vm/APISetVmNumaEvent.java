package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@RestResponse
public class APISetVmNumaEvent extends APIEvent {

    public APISetVmNumaEvent() {
    }

    public APISetVmNumaEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmNumaEvent __example__() {
        APISetVmNumaEvent event = new APISetVmNumaEvent();
        return event;
    }

}
