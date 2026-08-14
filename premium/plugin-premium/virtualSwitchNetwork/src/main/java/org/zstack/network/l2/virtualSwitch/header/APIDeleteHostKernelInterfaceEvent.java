package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteHostKernelInterfaceEvent extends APIEvent {

    public APIDeleteHostKernelInterfaceEvent() {
        super(null);
    }

    public APIDeleteHostKernelInterfaceEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteHostKernelInterfaceEvent __example__() {
        APIDeleteHostKernelInterfaceEvent event = new APIDeleteHostKernelInterfaceEvent();
        event.setSuccess(true);
        return event;
    }

}
