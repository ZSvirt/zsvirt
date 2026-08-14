package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:35
 */
@RestResponse
public class APIMoveResourcesToDirectoryEvent extends APIEvent {
    public APIMoveResourcesToDirectoryEvent() {
    }

    public APIMoveResourcesToDirectoryEvent(String apiId) {
        super(apiId);
    }

    public static APIMoveResourcesToDirectoryEvent __example__() {
        APIMoveResourcesToDirectoryEvent event = new APIMoveResourcesToDirectoryEvent();
        return event;
    }
}
