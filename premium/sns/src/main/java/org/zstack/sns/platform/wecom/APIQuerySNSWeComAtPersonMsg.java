package org.zstack.sns.platform.wecom;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSWeComAtPersonReply.class, inventoryClass = SNSWeComAtPersonInventory.class)
@RestRequest(path = "/sns/application-endpoints/we-com/at-persons",
        optionalPaths = {"/sns/application-endpoints/we-com/at-persons/{uuid}"},
        responseClass = APIQuerySNSWeComAtPersonReply.class, method = HttpMethod.GET)
public class APIQuerySNSWeComAtPersonMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=%s", uuid()), String.format("endpointUuid=%s", uuid()));
    }
}
