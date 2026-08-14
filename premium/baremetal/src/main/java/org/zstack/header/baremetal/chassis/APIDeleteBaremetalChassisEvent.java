package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 4/26/17.
 */
@RestResponse
public class APIDeleteBaremetalChassisEvent extends APIEvent {
    public APIDeleteBaremetalChassisEvent() {
    }

    public APIDeleteBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteBaremetalChassisEvent __example__() {
        return new APIDeleteBaremetalChassisEvent();
    }
}
