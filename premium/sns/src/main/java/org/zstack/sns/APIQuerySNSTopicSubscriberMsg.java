package org.zstack.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/sns/topics/subscribers",
        optionalPaths = {"/sns/topics/subscribers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySNSTopicSubscriberReply.class)
@AutoQuery(replyClass = APIQuerySNSTopicSubscriberReply.class, inventoryClass = SNSSubscriberInventory.class)
public class APIQuerySNSTopicSubscriberMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("topicUuid=3677dc0f00964b80886f3b2bbf9338cd");
    }
}
