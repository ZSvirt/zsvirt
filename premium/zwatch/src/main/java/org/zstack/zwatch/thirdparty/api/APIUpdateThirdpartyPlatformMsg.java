package org.zstack.zwatch.thirdparty.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;
import org.zstack.zwatch.thirdparty.msg.ThirdpartyPlatformMsg;

@RestRequest(
        path = "/zwatch/third-party/platforms/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateThirdpartyPlatformEvent.class,
        isAction = true
)
public class APIUpdateThirdpartyPlatformMsg extends APIMessage implements ThirdpartyPlatformMsg {
    @APIParam(resourceType = ThirdpartyPlatformVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam( maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 8192, required = false)
    private String template;

    @NoLogging
    @APIParam(maxLength = 2048, required = false)
    private String url;

    @APIParam(validValues = {"enable","disable"}, required = false)
    private String stateEvent;

    @APIParam(required = false)
    private Long lastSyncDateMills;

    @Override
    public String getThirdpartyPlatformUuid() {
        return uuid;
    }

    public static APIUpdateThirdpartyPlatformMsg __example__() {
        APIUpdateThirdpartyPlatformMsg msg = new APIUpdateThirdpartyPlatformMsg();

        msg.setUuid(uuid());
        msg.setName("test-group2");
        return msg;
    }

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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    public Long getLastSyncDateMills() {
        return lastSyncDateMills;
    }

    public void setLastSyncDateMills(Long lastSyncDateMills) {
        this.lastSyncDateMills = lastSyncDateMills;
    }
}
