package org.zstack.monitoring;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestResponse
public class APIDeleteAlertEvent extends APIEvent {
    public APIDeleteAlertEvent() {
    }

    public APIDeleteAlertEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteAlertEvent __example__() {
        APIDeleteAlertEvent evt = new APIDeleteAlertEvent();
        return evt;
    }
}
