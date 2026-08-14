package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by root on 7/29/16.
 */
@RestResponse
public class APISetVmUsbRedirectEvent extends APIEvent {
    public APISetVmUsbRedirectEvent() {
    }

    public APISetVmUsbRedirectEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmUsbRedirectEvent __example__() {
        APISetVmUsbRedirectEvent event = new APISetVmUsbRedirectEvent();
        return event;
    }

}
