package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:34
 */
@RestResponse
public class APIRemoveResourcesFromDirectoryEvent extends APIEvent {
    public APIRemoveResourcesFromDirectoryEvent() {
    }

    public APIRemoveResourcesFromDirectoryEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveResourcesFromDirectoryEvent __example__() {
        APIRemoveResourcesFromDirectoryEvent event = new APIRemoveResourcesFromDirectoryEvent();
        return event;
    }
}
