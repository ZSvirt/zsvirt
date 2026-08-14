package org.zstack.core.debug;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/7/25.
 */
@RestResponse
public class APIDebugSignalEvent extends APIEvent {
    public APIDebugSignalEvent() {
    }

    public APIDebugSignalEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDebugSignalEvent __example__() {
        APIDebugSignalEvent event = new APIDebugSignalEvent();


        return event;
    }

}
