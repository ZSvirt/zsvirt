package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:33
 */
@RestResponse
public class APIDeleteDirectoryEvent extends APIEvent {
    public APIDeleteDirectoryEvent() {
    }

    public APIDeleteDirectoryEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteDirectoryEvent __example__() {
        APIDeleteDirectoryEvent event = new APIDeleteDirectoryEvent();
        return event;
    }
}
