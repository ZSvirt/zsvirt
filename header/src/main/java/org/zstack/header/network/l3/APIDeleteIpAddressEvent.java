package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteIpAddressEvent extends APIEvent {

    public APIDeleteIpAddressEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteIpAddressEvent() {
        super(null);
    }

    public static APIDeleteIpAddressEvent __example__() {

        return new APIDeleteIpAddressEvent();
    }

}
