package org.zstack.header.protocol;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "routerId")
public class APISetVRouterRouterIdEvent extends APIEvent {
    private String routerId;

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public APISetVRouterRouterIdEvent() {
    }

    public APISetVRouterRouterIdEvent(String apiId) {
        super(apiId);
    }

    public static APISetVRouterRouterIdEvent __example__() {
        APISetVRouterRouterIdEvent event = new APISetVRouterRouterIdEvent();

        event.setRouterId("10.10.10.1");
        return event;
    }
}
