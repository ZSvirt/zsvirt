package org.zstack.sns;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Qi Le on 2019-07-16
 */
@RestResponse
public class APIValidateSNSSmsEndpointEvent extends APIEvent {
    public static APIValidateSNSSmsEndpointEvent __example__() {
        return new APIValidateSNSSmsEndpointEvent();
    }

    public APIValidateSNSSmsEndpointEvent() {
    }

    public APIValidateSNSSmsEndpointEvent(String apiId) {
        super(apiId);
    }
}
