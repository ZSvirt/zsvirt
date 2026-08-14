package org.zstack.monitoring.actions;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.monitoring.media.EmailMediaVO;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/7/7.
 */
@RestRequest(
        path = "/monitoring/trigger-actions/emails",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateMonitorTriggerActionEvent.class
)
@Deprecated
public class APICreateEmailMonitorTriggerActionMsg extends APICreateMonitorTriggerActionMsg {
    @APIParam(maxLength = 512)
    private String email;
    @APIParam(resourceType = EmailMediaVO.class)
    private String mediaUuid;

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

    @Override
    public String getType() {
        return MonitorTriggerActionConstants.EMAIL_MONITOR_TRIGGER_ACTION_TYPE;
    }

    public static APICreateEmailMonitorTriggerActionMsg __example__() {
        APICreateEmailMonitorTriggerActionMsg msg = new APICreateEmailMonitorTriggerActionMsg();
        msg.setName("email");
        msg.setMediaUuid(uuid());
        msg.setEmail("test@zstack.io");
        msg.setTriggerUuids(asList(uuid(),uuid()));
        return msg;
    }
}
