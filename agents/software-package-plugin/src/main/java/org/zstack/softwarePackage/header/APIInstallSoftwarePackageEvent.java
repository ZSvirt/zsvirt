package org.zstack.softwarePackage.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse()
public class APIInstallSoftwarePackageEvent extends APIEvent {

    public APIInstallSoftwarePackageEvent() {
    }

    public APIInstallSoftwarePackageEvent(String apiId) {
        super(apiId);
    }

    public static APIInstallSoftwarePackageEvent __example__() {
        return new APIInstallSoftwarePackageEvent();
    }
}
