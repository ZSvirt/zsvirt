package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/events/subscriptions/labels/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveLabelFromEventSubscriptionEvent.class
)
public class APIRemoveLabelFromEventSubscriptionMsg extends APIMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionLabelVO.class, successIfResourceNotExisting = true)
    private String uuid;
    @APINoSee
    private String subscriptionUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public static APIRemoveLabelFromEventSubscriptionMsg __example__() {
        APIRemoveLabelFromEventSubscriptionMsg ret = new APIRemoveLabelFromEventSubscriptionMsg();
        ret.uuid = uuid();
        return ret;
    }

    @Override
    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }
}
