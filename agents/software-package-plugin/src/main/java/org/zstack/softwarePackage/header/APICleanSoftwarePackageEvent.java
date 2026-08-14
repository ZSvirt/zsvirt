package org.zstack.softwarePackage.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APICleanSoftwarePackageEvent extends APIEvent {
    public APICleanSoftwarePackageEvent() {
    }

    public APICleanSoftwarePackageEvent(String apiId) {
        super(apiId);
    }

    public static APICleanSoftwarePackageEvent __example__() {
        return new APICleanSoftwarePackageEvent();
    }
}
