package org.zstack.monitoring.media;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestResponse
public class APIDeleteMediaEvent extends APIEvent {
    public APIDeleteMediaEvent() {
    }

    public APIDeleteMediaEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteMediaEvent __example__() {
        return new APIDeleteMediaEvent();
    }
}
