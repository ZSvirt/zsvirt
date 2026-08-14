package org.zstack.sns.platform.wecom;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSWeComEndpointReply.class, inventoryClass = SNSWeComEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints/we-com", optionalPaths = {"/sns/application-endpoints/we-com/{uuid}"},
        responseClass = APIQuerySNSWeComEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSWeComEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("url=http://wecom-url");
    }
}
