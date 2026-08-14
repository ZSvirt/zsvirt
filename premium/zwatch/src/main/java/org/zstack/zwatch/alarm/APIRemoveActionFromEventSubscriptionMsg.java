package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/events/subscriptions/{subscriptionUuid}/actions/{actionUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveActionFromEventSubscriptionEvent.class
)
public class APIRemoveActionFromEventSubscriptionMsg extends APIMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionVO.class)
    private String subscriptionUuid;
    @APIParam
    private String actionUuid;

    public static APIRemoveActionFromEventSubscriptionMsg __example__() {
        APIRemoveActionFromEventSubscriptionMsg ret = new APIRemoveActionFromEventSubscriptionMsg();
        ret.subscriptionUuid = uuid();
        ret.actionUuid = uuid();
        return ret;
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
    }
}
