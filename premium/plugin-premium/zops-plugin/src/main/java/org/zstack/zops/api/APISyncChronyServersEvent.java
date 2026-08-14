package org.zstack.zops.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISyncChronyServersEvent extends APIEvent {
    public APISyncChronyServersEvent() {
    }

    public APISyncChronyServersEvent(String apiId) {
        super(apiId);
    }

    public static APISyncChronyServersEvent __example__() {
        APISyncChronyServersEvent event = new APISyncChronyServersEvent();
        event.setSuccess(true);
        return event;
    }
}
