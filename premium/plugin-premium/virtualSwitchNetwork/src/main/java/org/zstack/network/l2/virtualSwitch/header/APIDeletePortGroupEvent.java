package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l3.APIDeleteL3NetworkEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeletePortGroupEvent extends APIDeleteL3NetworkEvent {
    public APIDeletePortGroupEvent() {
        super(null);
    }

    public APIDeletePortGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIDeletePortGroupEvent __example__() {
        APIDeletePortGroupEvent event = new APIDeletePortGroupEvent();
        event.setSuccess(true);
        return event;
    }
}
