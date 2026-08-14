package org.zstack.directory;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author shenjin
 * @date 2022/11/29 14:33
 */
@RestResponse
public class APIMoveDirectoryEvent extends APIEvent {
    public APIMoveDirectoryEvent() {
    }

    public APIMoveDirectoryEvent(String apiId) {
        super(apiId);
    }

    public static APIMoveDirectoryEvent __example__() {
        APIMoveDirectoryEvent event = new APIMoveDirectoryEvent();
        return event;
    }
}
