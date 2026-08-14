package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;

@RestRequest(
        path = "/zwatch/monitortemplates/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateMonitorTemplateEvent.class,
        isAction = true
)
public class APIUpdateMonitorTemplateMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = MonitorTemplateVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam( maxLength = 2048, required = false)
    private String description;

    public static APIUpdateMonitorTemplateMsg __example__() {
        APIUpdateMonitorTemplateMsg msg = new APIUpdateMonitorTemplateMsg();
        msg.setUuid(uuid());
        msg.setName("vm-template2");
        msg.setDescription("desc");
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(rsp.isSuccess() ? ((APIUpdateMonitorTemplateMsg)msg).getUuid() : "", MonitorTemplateVO.class);
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
}
