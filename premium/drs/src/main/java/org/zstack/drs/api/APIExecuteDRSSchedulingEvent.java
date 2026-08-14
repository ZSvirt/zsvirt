package org.zstack.drs.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Create by lining at 2019/12/12
 */
@RestResponse(fieldsTo = "all")
public class APIExecuteDRSSchedulingEvent extends APIEvent {
    public APIExecuteDRSSchedulingEvent(String apiId) {
        super(apiId);
    }

    public APIExecuteDRSSchedulingEvent() {
        super(null);
    }
}
