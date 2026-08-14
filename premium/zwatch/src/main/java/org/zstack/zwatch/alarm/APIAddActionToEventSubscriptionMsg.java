package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;

@RestRequest(path = "/zwatch/events/subscriptions/{subscriptionUuid}/actions",
        method = HttpMethod.POST,
        responseClass = APIAddActionToEventSubscriptionEvent.class,
        parameterName = "params")
public class APIAddActionToEventSubscriptionMsg extends APIMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionVO.class)
    private String subscriptionUuid;
    @APIParam
    private String actionUuid;
    @APIParam
    private String actionType;

    public static APIAddActionToEventSubscriptionMsg __example__() {
        APIAddActionToEventSubscriptionMsg ret = new APIAddActionToEventSubscriptionMsg();
        ret.subscriptionUuid = uuid();
        ret.actionUuid = uuid();
        ret.actionType = SNSActionFactory.type.toString();
        return ret;
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

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    @Override
    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }
}
