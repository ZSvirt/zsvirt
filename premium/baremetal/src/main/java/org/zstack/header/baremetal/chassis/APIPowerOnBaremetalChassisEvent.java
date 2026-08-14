package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestResponse
public class APIPowerOnBaremetalChassisEvent extends APIEvent{
    public APIPowerOnBaremetalChassisEvent() {
        super(null);
    }

    public APIPowerOnBaremetalChassisEvent(String apiId) {
        super(apiId);
    }

    public static APIPowerOnBaremetalChassisEvent __example__() {
        APIPowerOnBaremetalChassisEvent evt = new APIPowerOnBaremetalChassisEvent();
        evt.setSuccess(true);
        return evt;
    }
}
