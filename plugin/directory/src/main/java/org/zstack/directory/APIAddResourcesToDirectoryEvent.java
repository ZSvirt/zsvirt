package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:32
 */
@RestResponse
public class APIAddResourcesToDirectoryEvent extends APIEvent {
    public APIAddResourcesToDirectoryEvent(){
    }

    public APIAddResourcesToDirectoryEvent(String apiId) {
        super(apiId);
    }

    public static APIAddResourcesToDirectoryEvent __example__() {
        APIAddResourcesToDirectoryEvent event = new APIAddResourcesToDirectoryEvent();
        return event;
    }
}
