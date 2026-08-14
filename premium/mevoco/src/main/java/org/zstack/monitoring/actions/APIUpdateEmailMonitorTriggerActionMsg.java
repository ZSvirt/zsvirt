package org.zstack.monitoring.actions;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.monitoring.media.EmailMediaVO;

/**
 * Created by lining on 2017/8/23.
 */
@RestRequest(
        path = "/monitoring/trigger-actions/emails/{uuid}",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateMonitorTriggerActionEvent.class
)
@Deprecated
public class APIUpdateEmailMonitorTriggerActionMsg extends APIMessage {

    @APIParam(resourceType = EmailTriggerActionVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 512, required = false)
    private String email;

    @APIParam(resourceType = EmailMediaVO.class, required = false)
    private String mediaUuid;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMediaUuid() {
        return mediaUuid;
    }

    public void setMediaUuid(String mediaUuid) {
        this.mediaUuid = mediaUuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public static APIUpdateEmailMonitorTriggerActionMsg __example__() {
        APIUpdateEmailMonitorTriggerActionMsg msg = new APIUpdateEmailMonitorTriggerActionMsg();
        msg.setUuid(uuid());
        msg.setName("email");
        msg.setMediaUuid(uuid());
        msg.setEmail("test@zstack.io");
        return msg;
    }
}
