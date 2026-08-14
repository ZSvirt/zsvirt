package org.zstack.sns.platform.dingtalk;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSDingTalkAtPersonReply.class, inventoryClass = SNSDingTalkAtPersonInventory.class)
@RestRequest(path = "/sns/application-endpoints/ding-talk/at-persons",
        optionalPaths = {"/sns/application-endpoints/ding-talk/at-persons/{uuid}"},
        responseClass = APIQuerySNSDingTalkAtPersonReply.class, method = HttpMethod.GET)
public class APIQuerySNSDingTalkAtPersonMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=%s", uuid()), String.format("endpointUuid=%s", uuid()));
    }
}
