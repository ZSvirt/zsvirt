package org.zstack.zwatch.alarm.sns;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/alarms/sns/text-templates",
        optionalPaths = {"/zwatch/alarms/sns/text-templates/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySNSTextTemplateReply.class
)
@AutoQuery(replyClass = APIQuerySNSTextTemplateReply.class, inventoryClass = SNSTextTemplateInventory.class)
public class APIQuerySNSTextTemplateMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=test");
    }
}
