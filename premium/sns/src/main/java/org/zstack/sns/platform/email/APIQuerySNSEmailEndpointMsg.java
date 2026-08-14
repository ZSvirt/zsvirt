package org.zstack.sns.platform.email;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSEmailEndpointReply.class, inventoryClass = SNSEmailEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints/emails", optionalPaths = {"/sns/application-endpoints/emails/{uuid}"},
        responseClass = APIQuerySNSEmailEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSEmailEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("email=test@zstack.io");
    }
}
