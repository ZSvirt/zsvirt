package org.zstack.sns.platform.microsoftteams;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;
@RestResponse(allTo = "inventories")
public class APIQuerySNSMicrosoftTeamsEndpointReply extends APIQueryReply {
    private List<SNSMicrosoftTeamsEndpointInventory> inventories;

    public List<SNSMicrosoftTeamsEndpointInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SNSMicrosoftTeamsEndpointInventory> inventories) {
        this.inventories = inventories;
    }
}
