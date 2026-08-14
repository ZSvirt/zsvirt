package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;
import org.zstack.zwatch.monitorgroup.msg.MonitorGroupMsg;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/monitorgroups/{groupUuid}/actions",
        method = HttpMethod.POST,
        responseClass = APIAddInstanceToMonitorGroupEvent.class,
        parameterName = "params"
)
public class APIAddInstanceToMonitorGroupMsg extends APICreateMessage implements APIAuditor, MonitorGroupMsg {
    @APIParam(maxLength = 32, resourceType = ResourceVO.class)
    private String instanceUuid;

    @APIParam(resourceType = MonitorGroupVO.class, maxLength = 32)
    private String groupUuid;

    public static APIAddInstanceToMonitorGroupMsg __example__() {
        APIAddInstanceToMonitorGroupMsg msg = new APIAddInstanceToMonitorGroupMsg();
        msg.setGroupUuid(uuid());
        msg.setInstanceUuid(uuid());
        return msg;
    }

    @Override
    public String getMonitorGroupUuid() {
        return groupUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddInstanceToMonitorGroupEvent)rsp).getInventory().getGroupUuid() : "", MonitorGroupVO.class);
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
