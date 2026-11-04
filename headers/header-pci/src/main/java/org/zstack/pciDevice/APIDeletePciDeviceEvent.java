package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse
public class APIDeletePciDeviceEvent extends APIEvent {
    public APIDeletePciDeviceEvent() {
        super(null);
    }

    public APIDeletePciDeviceEvent(String apiId) {
        super(apiId);
    }

    public static APIDeletePciDeviceEvent __example__() {
        APIDeletePciDeviceEvent event = new APIDeletePciDeviceEvent();
        event.setSuccess(true);

        return event;
    }
}