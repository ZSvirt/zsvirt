package org.zstack.softwarePackage.header;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APICleanUpgradeSoftwarePackageEvent extends APIEvent {
    public APICleanUpgradeSoftwarePackageEvent() {
    }

    public APICleanUpgradeSoftwarePackageEvent(String apiId) {
        super(apiId);
    }

    public static APICleanUpgradeSoftwarePackageEvent __example__() {
        return new APICleanUpgradeSoftwarePackageEvent();
    }
}
