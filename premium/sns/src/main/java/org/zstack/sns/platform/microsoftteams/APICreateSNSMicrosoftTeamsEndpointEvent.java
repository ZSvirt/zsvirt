package org.zstack.sns.platform.microsoftteams;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateSNSMicrosoftTeamsEndpointEvent extends APIEvent {
    private SNSMicrosoftTeamsEndpointInventory inventory;

    public static APICreateSNSMicrosoftTeamsEndpointEvent __example__() {
        APICreateSNSMicrosoftTeamsEndpointEvent evt = new APICreateSNSMicrosoftTeamsEndpointEvent();
        evt.setInventory(SNSMicrosoftTeamsEndpointInventory.__example1__());
        return evt;
    }

    public APICreateSNSMicrosoftTeamsEndpointEvent() {
    }

    public APICreateSNSMicrosoftTeamsEndpointEvent(String apiId) {
        super(apiId);
    }

    public SNSMicrosoftTeamsEndpointInventory getInventory() {
        return inventory;
    }

    public void setInventory(SNSMicrosoftTeamsEndpointInventory inventory) {
        this.inventory = inventory;
    }
}
