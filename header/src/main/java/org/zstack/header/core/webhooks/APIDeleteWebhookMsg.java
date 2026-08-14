package org.zstack.header.core.webhooks;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/5/7.
 */
@RestRequest(
        path = "/web-hooks/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteWebhookEvent.class
)
@Deprecated
public class APIDeleteWebhookMsg extends APIDeleteMessage {
    @APIParam(resourceType = WebhookVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteWebhookMsg __example__() {
        APIDeleteWebhookMsg msg = new APIDeleteWebhookMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
