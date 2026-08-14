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
        path = "/zwatch/monitorgroups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMonitorGroupEvent.class
)
public class APIDeleteMonitorGroupMsg extends APIDeleteMessage implements APIAuditor, MonitorGroupMsg{
    @APIParam(resourceType = MonitorGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteMonitorGroupMsg __example__() {
        APIDeleteMonitorGroupMsg msg = new APIDeleteMonitorGroupMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIDeleteMonitorGroupMsg)msg).getUuid(), MonitorGroupVO.class);
    }

    @Override
    public String getMonitorGroupUuid() {
        return uuid;
    }
}
