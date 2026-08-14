package org.zstack.sns.platform.feishu;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSFeiShuEndpointReply.class, inventoryClass = SNSFeiShuEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints/feishu", optionalPaths = {"/sns/application-endpoints/feishu/{uuid}"},
        responseClass = APIQuerySNSFeiShuEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSFeiShuEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("url=http://feishu-url");
    }
}
