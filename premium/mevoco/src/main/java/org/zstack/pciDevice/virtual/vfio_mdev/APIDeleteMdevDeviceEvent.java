package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @author yu.sun
 * @date 2022/12/07 15:46
 **/
@RestResponse
public class APIDeleteMdevDeviceEvent extends APIEvent {
    public APIDeleteMdevDeviceEvent() {
        super();
    }

    public APIDeleteMdevDeviceEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteMdevDeviceEvent __example__() {
        APIDeleteMdevDeviceEvent event = new APIDeleteMdevDeviceEvent();
        event.setSuccess(true);
        return event;
    }
}