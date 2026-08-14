package org.zstack.sns.platform.email;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIValidateSNSEmailPlatformEvent extends APIEvent {
    public static APIValidateSNSEmailPlatformEvent __example__() {
        return new APIValidateSNSEmailPlatformEvent();
    }

    public APIValidateSNSEmailPlatformEvent() {
    }

    public APIValidateSNSEmailPlatformEvent(String apiId) {
        super(apiId);
    }

}
