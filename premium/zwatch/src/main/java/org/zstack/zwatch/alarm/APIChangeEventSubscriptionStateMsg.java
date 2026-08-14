package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author shenjin
 * @date 2023/5/16 13:10
 */
@RestRequest(
        path = "/zwatch/change/eventSubscription/{uuid}/state",
        method = HttpMethod.PUT,
        responseClass = APIChangeEventSubscriptionStateEvent.class,
        isAction = true
)
public class APIChangeEventSubscriptionStateMsg extends APIMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionVO.class)
    private String uuid;

    @APIParam(validValues = {"Enabled","Disabled"})
    private String state;

    public static APIChangeEventSubscriptionStateMsg __example__() {
        APIChangeEventSubscriptionStateMsg ret = new APIChangeEventSubscriptionStateMsg();
        ret.setUuid(uuid());
        ret.setState(EventSubscriptionState.Disabled);
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public EventSubscriptionState getState() {
        return EventSubscriptionState.valueOf(state);
    }

    public void setState(EventSubscriptionState state) {
        this.state = state.toString();
    }

    @Override
    public String getSubscriptionUuid() {
        return uuid;
    }
}
