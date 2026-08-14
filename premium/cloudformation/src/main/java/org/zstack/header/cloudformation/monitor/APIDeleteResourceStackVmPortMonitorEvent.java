package org.zstack.header.cloudformation.monitor;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@RestResponse
public class APIDeleteResourceStackVmPortMonitorEvent extends APIEvent {
    public APIDeleteResourceStackVmPortMonitorEvent() {
    }

    public APIDeleteResourceStackVmPortMonitorEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteResourceStackVmPortMonitorEvent __example__() {
        APIDeleteResourceStackVmPortMonitorEvent event = new APIDeleteResourceStackVmPortMonitorEvent();
        return event;
    }
}
