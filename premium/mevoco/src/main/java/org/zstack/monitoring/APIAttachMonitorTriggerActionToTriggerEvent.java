package org.zstack.monitoring;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/12.
 */
@RestResponse
public class APIAttachMonitorTriggerActionToTriggerEvent extends APIEvent {
    public APIAttachMonitorTriggerActionToTriggerEvent() {
    }

    public APIAttachMonitorTriggerActionToTriggerEvent(String apiId) {
        super(apiId);
    }
}
