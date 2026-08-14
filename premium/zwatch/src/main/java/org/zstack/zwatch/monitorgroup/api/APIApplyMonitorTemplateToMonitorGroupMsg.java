package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;
import org.zstack.zwatch.monitorgroup.msg.MonitorGroupMsg;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/monitortemplates/{templateUuid}/monitorgroups/{groupUuid}",
        method = HttpMethod.POST,
        responseClass = APIApplyMonitorTemplateToMonitorGroupEvent.class,
        parameterName = "params"
)
public class APIApplyMonitorTemplateToMonitorGroupMsg extends APIMessage implements APIAuditor, MonitorGroupMsg {
    @APIParam(maxLength = 32, resourceType = MonitorTemplateVO.class)
    private String templateUuid;

    @APIParam(maxLength = 32, resourceType = MonitorGroupVO.class)
    private String groupUuid;

    public static APIApplyMonitorTemplateToMonitorGroupMsg __example__() {
        APIApplyMonitorTemplateToMonitorGroupMsg msg = new APIApplyMonitorTemplateToMonitorGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setTemplateUuid(uuid());
        return msg;
    }

    @Override
    public String getMonitorGroupUuid() {
        return groupUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIApplyMonitorTemplateToMonitorGroupMsg)msg).getGroupUuid() : "", MonitorGroupVO.class);
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
