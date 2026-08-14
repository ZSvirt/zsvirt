package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = "all")
public class APIMountBlockDeviceEvent extends APIEvent {

    public APIMountBlockDeviceEvent() {
        super(null);
    }

    public APIMountBlockDeviceEvent(String apiId) {
        super(apiId);
    }

    public static APIMountBlockDeviceEvent __example__() {
        return new APIMountBlockDeviceEvent();
    }
}
