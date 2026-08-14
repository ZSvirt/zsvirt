package org.zstack.softwarePackage.header;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIUninstallSoftwarePackageEvent extends APIEvent {
    public APIUninstallSoftwarePackageEvent() {
    }

    public APIUninstallSoftwarePackageEvent(String apiId) {
        super(apiId);
    }

    public static APIUninstallSoftwarePackageEvent __example__() {
        return new APIUninstallSoftwarePackageEvent();
    }
}
