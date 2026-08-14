package org.zstack.sdk.sns.platform.microsoftteams;

import org.zstack.sdk.sns.platform.microsoftteams.SNSMicrosoftTeamsEndpointInventory;

public class CreateSNSMicrosoftTeamsEndpointResult {
    public SNSMicrosoftTeamsEndpointInventory inventory;
    public void setInventory(SNSMicrosoftTeamsEndpointInventory inventory) {
        this.inventory = inventory;
    }
    public SNSMicrosoftTeamsEndpointInventory getInventory() {
        return this.inventory;
    }

}
