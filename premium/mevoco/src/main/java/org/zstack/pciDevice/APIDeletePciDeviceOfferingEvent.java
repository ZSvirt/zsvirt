package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse
public class APIDeletePciDeviceOfferingEvent extends APIEvent {
    public APIDeletePciDeviceOfferingEvent() {
    }

    public APIDeletePciDeviceOfferingEvent(String apiId) {
        super(apiId);
    }

    public static APIDeletePciDeviceOfferingEvent __example__() {
        APIDeletePciDeviceOfferingEvent event = new APIDeletePciDeviceOfferingEvent();
        return event;
    }
}
