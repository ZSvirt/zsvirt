package org.zstack.sns.platform.microsoftteams;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSMicrosoftTeamsEndpointReply.class, inventoryClass = SNSMicrosoftTeamsEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints/microsoft-teams", optionalPaths = {"/sns/application-endpoints/microsoft-teams/{uuid}"},
        responseClass = APIQuerySNSMicrosoftTeamsEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSMicrosoftTeamsEndpointMsg  extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("url=http://teams-url");
    }
}
