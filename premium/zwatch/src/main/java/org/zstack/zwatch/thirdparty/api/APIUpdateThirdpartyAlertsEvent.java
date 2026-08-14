package org.zstack.zwatch.thirdparty.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIUpdateThirdpartyAlertsEvent extends APIEvent {
    public APIUpdateThirdpartyAlertsEvent() {
    }

    public APIUpdateThirdpartyAlertsEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateThirdpartyAlertsEvent __example__() {
        APIUpdateThirdpartyAlertsEvent reply = new APIUpdateThirdpartyAlertsEvent();
        return reply;
    }
}
