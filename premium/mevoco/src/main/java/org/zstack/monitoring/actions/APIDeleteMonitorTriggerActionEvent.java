package org.zstack.monitoring.actions;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/6/11.
 */
@RestResponse
public class APIDeleteMonitorTriggerActionEvent extends APIEvent {
    public APIDeleteMonitorTriggerActionEvent() {
    }

    public APIDeleteMonitorTriggerActionEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteMonitorTriggerActionEvent __example__() {
        return new APIDeleteMonitorTriggerActionEvent();
    }
}
