package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Wenhao.Zhang on 22/06/15
 */
@RestResponse
public class APISyncVmClockEvent extends APIEvent {
    public APISyncVmClockEvent(String apiId) {
        super(apiId);
    }

    public APISyncVmClockEvent() {
        super();
    }

    public static APISetVmCleanTrafficEvent __example__() {
        return new APISetVmCleanTrafficEvent();
    }
}
