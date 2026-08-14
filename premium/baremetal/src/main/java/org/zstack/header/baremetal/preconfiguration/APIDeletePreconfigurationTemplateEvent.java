package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2018-12-26.
 */
@RestResponse
public class APIDeletePreconfigurationTemplateEvent extends APIEvent {
    public APIDeletePreconfigurationTemplateEvent() {
    }

    public APIDeletePreconfigurationTemplateEvent(String apiId) {
        super(apiId);
    }

    public static APIDeletePreconfigurationTemplateEvent __example__() {
        APIDeletePreconfigurationTemplateEvent event = new APIDeletePreconfigurationTemplateEvent();
        return event;
    }
}
