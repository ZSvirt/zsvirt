package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/events/subscriptions/{uuid}", method = HttpMethod.DELETE, responseClass = APIUnsubscribeEventEvent.class)
public class APIUnsubscribeEventMsg extends APIDeleteMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public static APIUnsubscribeEventMsg __example__() {
        APIUnsubscribeEventMsg ret = new APIUnsubscribeEventMsg();
        ret.setUuid(uuid());
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSubscriptionUuid() {
        return uuid;
    }
}
