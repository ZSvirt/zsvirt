package org.zstack.pciDevice;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 10/07/2017.
 */
@RestResponse(allTo = "state")
public class APIUpdateHostIommuStateEvent extends APIEvent {
    private HostIommuStateType state;

    public APIUpdateHostIommuStateEvent() {
    }

    public APIUpdateHostIommuStateEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateHostIommuStateEvent __example__() {
        APIUpdateHostIommuStateEvent event = new APIUpdateHostIommuStateEvent();
        event.setState(HostIommuStateType.Enabled);
        return event;
    }

    public HostIommuStateType getState() {
        return state;
    }

    public void setState(HostIommuStateType state) {
        this.state = state;
    }
}
