package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSTopicReply.class, inventoryClass = SNSTopicInventory.class)
@RestRequest(path = "/sns/topics", optionalPaths = {"/sns/topics/{uuid}"},
        responseClass = APIQuerySNSTopicReply.class, method = HttpMethod.GET)
public class APIQuerySNSTopicMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=api");
    }
}
