package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Created by weiwang on 18/05/2017.
 */
@RestResponse(fieldsTo = {"all"})
public class APISetL3NetworkMtuEvent extends APIEvent {
    public APISetL3NetworkMtuEvent() {
    }

    public APISetL3NetworkMtuEvent(String apiId) {
        super(apiId);
    }

    public static APISetL3NetworkMtuEvent __example__() {
        APISetL3NetworkMtuEvent event = new APISetL3NetworkMtuEvent();
        event.setSuccess(true);
        return event;
    }
}
