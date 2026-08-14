package org.zstack.header.core.webhooks;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/5/7.
 */
@RestResponse
public class APIDeleteWebhookEvent extends APIEvent {
    public APIDeleteWebhookEvent() {
    }

    public APIDeleteWebhookEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteWebhookEvent __example__() {
        return new APIDeleteWebhookEvent();
    }
}
