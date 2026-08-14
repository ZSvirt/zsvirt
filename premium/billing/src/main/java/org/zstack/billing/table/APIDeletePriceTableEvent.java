package org.zstack.billing.table;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/9/10.
 */
@RestResponse
public class APIDeletePriceTableEvent extends APIEvent {
    public APIDeletePriceTableEvent() {
    }

    public APIDeletePriceTableEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeletePriceTableEvent __example__() {
        APIDeletePriceTableEvent event = new APIDeletePriceTableEvent();


        return event;
    }

}
