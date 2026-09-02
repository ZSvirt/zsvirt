package org.zstack.accessKey;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteAccessKeyEvent extends APIEvent {

    public static APIDeleteAccessKeyEvent __example__() {
        APIDeleteAccessKeyEvent ret = new APIDeleteAccessKeyEvent();
        return ret;
    }

    public APIDeleteAccessKeyEvent() {
    }

    public APIDeleteAccessKeyEvent(String apiId) {
        super(apiId);
    }
}
