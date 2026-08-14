package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;
import org.zstack.zwatch.monitorgroup.msg.MonitorGroupMsg;

@RestRequest(
        path = "/zwatch/monitorgroups/{groupUuid}/actions/{instanceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveInstanceFromMonitorGroupEvent.class
)
public class APIRemoveInstanceFromMonitorGroupMsg extends APIDeleteMessage implements APIAuditor, MonitorGroupMsg{
    @APIParam(resourceType = MonitorGroupVO.class)
    private String groupUuid;

    @APIParam
    private String instanceUuid;

    public static APIRemoveInstanceFromMonitorGroupMsg __example__() {
        APIRemoveInstanceFromMonitorGroupMsg msg = new APIRemoveInstanceFromMonitorGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setInstanceUuid(uuid());
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIRemoveInstanceFromMonitorGroupMsg)msg).getGroupUuid(), MonitorGroupVO.class);
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

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }
}
