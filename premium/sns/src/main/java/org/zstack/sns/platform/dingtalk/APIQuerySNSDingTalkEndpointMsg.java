package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSDingTalkEndpointReply.class, inventoryClass = SNSDingTalkEndpointInventory.class)
@RestRequest(path = "/sns/application-endpoints/ding-talk", optionalPaths = {"/sns/application-endpoints/ding-talk/{uuid}"},
        responseClass = APIQuerySNSDingTalkEndpointReply.class, method = HttpMethod.GET)
public class APIQuerySNSDingTalkEndpointMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("url=http://dingding-url");
    }
}
