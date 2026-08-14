package org.zstack.header.core.webhooks;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2017/5/7.
 */
@AutoQuery(inventoryClass = WebhookInventory.class, replyClass = APIQueryWebhookReply.class)
@RestRequest(
        path = "/web-hooks",
        optionalPaths = {"/web-hooks/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryWebhookReply.class
)
@Deprecated
public class APIQueryWebhookMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return Arrays.asList("name=test");
    }
}
