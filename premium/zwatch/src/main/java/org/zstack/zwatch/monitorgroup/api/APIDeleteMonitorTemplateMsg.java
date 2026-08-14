package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;

@RestRequest(
        path = "/zwatch/monitortemplates/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMonitorTemplateEvent.class
)
public class APIDeleteMonitorTemplateMsg extends APIDeleteMessage implements APIAuditor{
    @APIParam(resourceType = MonitorTemplateVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteMonitorTemplateMsg __example__() {
        APIDeleteMonitorTemplateMsg msg = new APIDeleteMonitorTemplateMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIDeleteMonitorTemplateMsg)msg).getUuid(), MonitorTemplateVO.class);
    }
}
