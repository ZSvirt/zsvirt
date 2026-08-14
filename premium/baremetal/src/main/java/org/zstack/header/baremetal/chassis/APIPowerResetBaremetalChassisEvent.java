package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestResponse
public class APIPowerResetBaremetalChassisEvent extends APIEvent{
    public APIPowerResetBaremetalChassisEvent() {
        super(null);
    }

    public APIPowerResetBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIPowerResetBaremetalChassisEvent __example__() {
        APIPowerResetBaremetalChassisEvent evt = new APIPowerResetBaremetalChassisEvent();
        evt.setSuccess(true);
        return evt;
    }
}
