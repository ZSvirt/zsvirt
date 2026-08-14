package org.zstack.log;

import org.zstack.core.Platform;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteLogConfigurationEvent extends APIEvent {
    public APIDeleteLogConfigurationEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteLogConfigurationEvent() {
        super(null);
    }

    public static APIDeleteLogConfigurationEvent __example__() {
        APIDeleteLogConfigurationEvent evt = new APIDeleteLogConfigurationEvent();
        return evt;
    }
}
