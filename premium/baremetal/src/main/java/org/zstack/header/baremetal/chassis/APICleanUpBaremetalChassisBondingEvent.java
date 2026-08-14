package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-01-20.
 */
@RestResponse
public class APICleanUpBaremetalChassisBondingEvent extends APIEvent {
    public APICleanUpBaremetalChassisBondingEvent() {
    }

    public APICleanUpBaremetalChassisBondingEvent(String apiId) {
        super(apiId);
    }

    public static APICleanUpBaremetalChassisBondingEvent __example__() {
        return new APICleanUpBaremetalChassisBondingEvent();
    }
}
