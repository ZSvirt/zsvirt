package org.zstack.storage.backup.imagestore;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISyncImageEvent extends APIEvent {
    public APISyncImageEvent(String apiId) {
        super(apiId);
    }

    public APISyncImageEvent() {
        super();
    }

    public static APISyncImageEvent __example__() {
        APISyncImageEvent event = new APISyncImageEvent(Platform.getUuid());
        return event;
    }
}
