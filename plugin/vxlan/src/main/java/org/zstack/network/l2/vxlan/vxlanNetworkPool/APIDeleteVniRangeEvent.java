package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 03/05/2017.
 */

@RestResponse
public class APIDeleteVniRangeEvent extends APIEvent {
    public APIDeleteVniRangeEvent() {
    }

    public APIDeleteVniRangeEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteVniRangeEvent __example__() {
        APIDeleteVniRangeEvent event = new APIDeleteVniRangeEvent();

        return event;
    }

}