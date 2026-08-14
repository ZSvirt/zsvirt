package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/monitortemplates/{uuid}/actions",
        method = HttpMethod.POST,
        responseClass = APICloneMonitorTemplateEvent.class,
        parameterName = "params"
)
public class APICloneMonitorTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = MonitorTemplateVO.class)
    private String uuid;

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;


    public static APICloneMonitorTemplateMsg __example__() {
        APICloneMonitorTemplateMsg msg = new APICloneMonitorTemplateMsg();
        msg.setName("clone-by-template");
        msg.setUuid(uuid());
        msg.setDescription("desc");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateMonitorTemplateEvent)rsp).getInventory().getUuid() : "", MonitorTemplateVO.class);
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
