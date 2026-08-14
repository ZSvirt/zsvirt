package org.zstack.header.baremetal.instance;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/4/18.
 */
@RestResponse
public class APIDestroyBaremetalInstanceEvent extends APIEvent {
    public APIDestroyBaremetalInstanceEvent() {
    }

    public APIDestroyBaremetalInstanceEvent(String apiId) {
        super(apiId);
    }

    public static APIDestroyBaremetalInstanceEvent __example__() {
        return new APIDestroyBaremetalInstanceEvent();
    }
}
