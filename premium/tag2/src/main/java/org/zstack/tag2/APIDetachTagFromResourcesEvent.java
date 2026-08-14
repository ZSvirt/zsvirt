package org.zstack.tag2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachTagFromResourcesEvent extends APIEvent {
    public APIDetachTagFromResourcesEvent(String apiId) {
        super(apiId);
    }

    public APIDetachTagFromResourcesEvent() {
        super();
    }
}
