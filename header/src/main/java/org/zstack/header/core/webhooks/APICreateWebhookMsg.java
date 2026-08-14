package org.zstack.header.core.webhooks;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2017/5/7.
 */
@RestRequest(
        path = "/web-hooks",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateWebhookEvent.class
)
@Deprecated
public class APICreateWebhookMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 2048)
    private String url;
    @APIParam(maxLength = 255)
    private String type;
    private String opaque;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOpaque() {
        return opaque;
    }

    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }

    public static APICreateWebhookMsg __example__() {
        APICreateWebhookMsg msg = new APICreateWebhookMsg();
        msg.setName("test");
        msg.setUrl("http://localhost/web-hooks");
        msg.setType("CanonicalEvent");
        return msg;
    }
}
