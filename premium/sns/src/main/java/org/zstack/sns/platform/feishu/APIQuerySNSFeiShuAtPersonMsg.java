package org.zstack.sns.platform.feishu;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySNSFeiShuAtPersonReply.class, inventoryClass = SNSFeiShuAtPersonInventory.class)
@RestRequest(path = "/sns/application-endpoints/feishu/at-persons",
        optionalPaths = {"/sns/application-endpoints/feishu/at-persons/{uuid}"},
        responseClass = APIQuerySNSFeiShuAtPersonReply.class, method = HttpMethod.GET)
public class APIQuerySNSFeiShuAtPersonMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList(String.format("uuid=%s", uuid()), String.format("endpointUuid=%s", uuid()));
    }
}
