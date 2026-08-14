package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteLoadBalancerServerGroupEvent extends APIEvent{
    public APIDeleteLoadBalancerServerGroupEvent() {
    }

    public APIDeleteLoadBalancerServerGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteLoadBalancerServerGroupEvent __example__() {
        APIDeleteLoadBalancerServerGroupEvent event = new APIDeleteLoadBalancerServerGroupEvent();
        event.setSuccess(true);
        return event;
    }
}
