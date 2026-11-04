package org.zstack.zwatch.resnotify;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.header.ZWatchConstants;

@Action(category = ZWatchConstants.CATEGORY)
@AutoQuery(replyClass = APIQueryResNotifySubscriptionReply.class, inventoryClass = ResNotifySubscriptionInventory.class)
@RestRequest(
    path = "/zwatch/resnotify/subscriptions", 
    optionalPaths = {"/zwatch/resnotify/subscriptions/{uuid}"}, 
    method = HttpMethod.GET, 
    responseClass = APIQueryResNotifySubscriptionReply.class
)
public class APIQueryResNotifySubscriptionMsg extends APIQueryMessage {
    public static APIQueryResNotifySubscriptionMsg __example__() {
        return new APIQueryResNotifySubscriptionMsg();
    }
}
