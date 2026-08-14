package org.zstack.vrouterRoute;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 15/06/2017.
 */
@RestResponse
public class APIDeleteVRouterRouteTableEvent extends APIEvent {
    public APIDeleteVRouterRouteTableEvent() {
        super(null);
    }

    public APIDeleteVRouterRouteTableEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVRouterRouteTableEvent __example__() {
        APIDeleteVRouterRouteTableEvent event = new APIDeleteVRouterRouteTableEvent();
        event.setSuccess(true);

        return event;
    }
}