package org.zstack.zwatch.resnotify;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.ZWatchConstants;

import java.util.Arrays;
import java.util.List;



@Action(category = ZWatchConstants.CATEGORY)
@RestRequest(
    path = "/zwatch/resnotify/subscriptions/{uuid}/actions",
    isAction = true,
    method = HttpMethod.PUT,
    responseClass = APIUpdateResNotifySubscriptionEvent.class
)
public class APIUpdateResNotifySubscriptionMsg extends APIMessage implements APIAuditor {

    @APIParam(resourceType = ResNotifySubscriptionVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 1024, required = false)
    private String description;

    @APIParam(required = false)
    private List<String> resourceTypes;

    @APIParam(required = false, validValues = { "CREATE", "UPDATE", "DELETE" })
    private List<String> eventTypes;

    @APIParam(required = false, validValues = { "Enabled", "Disabled" })
    private String state;
    @APIParam(maxLength = 2048, required = false)
    @NoLogging(type = NoLogging.Type.Uri)
    private String webhookUrl;

    @APIParam(maxLength = 256, required = false)
    @NoLogging
    private String secret;

    @APIParam(maxLength = 2048, required = false)
    @NoLogging
    private String customHeaders;

    public static APIUpdateResNotifySubscriptionMsg __example__() {
        APIUpdateResNotifySubscriptionMsg msg = new APIUpdateResNotifySubscriptionMsg();
        msg.setUuid("subscription-uuid");
        msg.setResourceTypes(Arrays.asList("VmInstanceVO", "HostVO", "VolumeVO", "ClusterVO"));
        msg.setEventTypes(Arrays.asList("CREATE", "UPDATE", "DELETE"));
        return msg;
    }

    // Getters and Setters

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
        return new Result(uuid, ResNotifySubscriptionVO.class);
    }
}
