package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestResponse
public class APIPowerOffBaremetalChassisEvent extends APIEvent{
    public APIPowerOffBaremetalChassisEvent() {
        super(null);
    }

    public APIPowerOffBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIPowerOffBaremetalChassisEvent __example__() {
        APIPowerOffBaremetalChassisEvent evt = new APIPowerOffBaremetalChassisEvent();
        evt.setSuccess(true);
        return evt;
    }
}
