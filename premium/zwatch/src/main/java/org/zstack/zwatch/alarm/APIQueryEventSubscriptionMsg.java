package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryEventSubscriptionReply.class, inventoryClass = EventSubscriptionInventory.class)
@RestRequest(path = "/zwatch/events/subscriptions", optionalPaths = {"/zwatch/events/subscriptions/{uuid}"},
        responseClass = APIQueryEventSubscriptionReply.class, method = HttpMethod.GET)
public class APIQueryEventSubscriptionMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=test");
    }
}
