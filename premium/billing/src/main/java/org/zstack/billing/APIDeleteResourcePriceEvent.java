package org.zstack.billing;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/6/12.
 */
@RestResponse
public class APIDeleteResourcePriceEvent extends APIEvent {
    public APIDeleteResourcePriceEvent() {
    }

    public APIDeleteResourcePriceEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteResourcePriceEvent __example__() {
        APIDeleteResourcePriceEvent event = new APIDeleteResourcePriceEvent();


        return event;
    }

}
