package org.zstack.storage.device.nvme;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteNvmeServerEvent extends APIEvent {
    public APIDeleteNvmeServerEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteNvmeServerEvent() {
    }

    public static APIDeleteNvmeServerEvent __example__() {
        APIDeleteNvmeServerEvent event = new APIDeleteNvmeServerEvent();
        return event;
    }
}
