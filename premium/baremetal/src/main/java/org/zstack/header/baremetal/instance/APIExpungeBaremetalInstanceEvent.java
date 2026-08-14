package org.zstack.header.baremetal.instance;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/4/18.
 */
@RestResponse
public class APIExpungeBaremetalInstanceEvent extends APIEvent {
    public APIExpungeBaremetalInstanceEvent() {
    }

    public APIExpungeBaremetalInstanceEvent(String apiId) {
        super(apiId);
    }

    public static APIExpungeBaremetalInstanceEvent __example__() {
        return new APIExpungeBaremetalInstanceEvent();
    }
}
