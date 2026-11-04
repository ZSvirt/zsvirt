package org.zstack.zwatch.resnotify;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.header.ZWatchConstants;

import java.util.Arrays;
import java.util.List;



@Action(category = ZWatchConstants.CATEGORY)
@RestRequest(
    path = "/zwatch/resnotify/subscriptions", 
    method = HttpMethod.POST, 
    responseClass = APISubscribeResNotifyEvent.class, 
    parameterName = "params"
)
public class APISubscribeResNotifyMsg extends APICreateMessage implements APIAuditor {

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 1024, required = false)
    private String description;

    @APIParam(required = false)
    private List<String> resourceTypes;

    @APIParam(required = false, validValues = { "CREATE", "UPDATE", "DELETE" })
    private List<String> eventTypes;

    @APIParam(required = false, validValues = { "WEBHOOK" })
    private String type = "WEBHOOK";
    @APIParam(maxLength = 2048)
    @NoLogging(type = NoLogging.Type.Uri)
    private String webhookUrl;

    @APIParam(maxLength = 256, required = false)
    @NoLogging
    private String secret;

    @APIParam(maxLength = 2048, required = false)
    @NoLogging
    private String customHeaders;

    public static APISubscribeResNotifyMsg __example__() {
        APISubscribeResNotifyMsg msg = new APISubscribeResNotifyMsg();
        msg.setName("zcf-sync");
        msg.setResourceTypes(Arrays.asList("VmInstanceVO", "HostVO", "VolumeVO"));
        msg.setEventTypes(Arrays.asList("CREATE", "UPDATE", "DELETE"));
        msg.setWebhookUrl("http://example.com/webhook");
        return msg;
    }

    // Getters and Setters

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

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public List<String> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(List<String> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(String customHeaders) {
        this.customHeaders = customHeaders;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APISubscribeResNotifyEvent) rsp).getInventory().getUuid() : "",
                ResNotifySubscriptionVO.class);
    }
}
