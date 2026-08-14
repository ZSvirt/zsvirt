package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInventory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;
import org.zstack.zwatch.monitorgroup.msg.MonitorGroupMsg;

@RestRequest(
        path = "/zwatch/monitortemplates/{templateUuid}/monitorgroups/{groupUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRevokeMonitorTemplateFromMonitorGroupEvent.class
)
public class APIRevokeMonitorTemplateFromMonitorGroupMsg extends APIDeleteMessage implements APIAuditor, MonitorGroupMsg{
    @APIParam(resourceType = MonitorGroupVO.class)
    private String groupUuid;

    @APIParam(resourceType = MonitorTemplateVO.class)
    private String templateUuid;

    public static APIRevokeMonitorTemplateFromMonitorGroupMsg __example__() {
        APIRevokeMonitorTemplateFromMonitorGroupMsg msg = new APIRevokeMonitorTemplateFromMonitorGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setTemplateUuid(uuid());
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIRevokeMonitorTemplateFromMonitorGroupMsg)msg).getGroupUuid(), MonitorGroupVO.class);
    }

    @Override
    public String getMonitorGroupUuid() {
        return groupUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }
}
