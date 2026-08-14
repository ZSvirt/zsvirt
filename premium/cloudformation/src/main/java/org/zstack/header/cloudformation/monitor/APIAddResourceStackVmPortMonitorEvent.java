package org.zstack.header.cloudformation.monitor;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@RestResponse
public class APIAddResourceStackVmPortMonitorEvent extends APIEvent {
    public APIAddResourceStackVmPortMonitorEvent() {
    }

    public APIAddResourceStackVmPortMonitorEvent(String apiId) {
        super(apiId);
    }

    public static APIAddResourceStackVmPortMonitorEvent __example__() {
        APIAddResourceStackVmPortMonitorEvent event = new APIAddResourceStackVmPortMonitorEvent();
        return event;
    }
}
