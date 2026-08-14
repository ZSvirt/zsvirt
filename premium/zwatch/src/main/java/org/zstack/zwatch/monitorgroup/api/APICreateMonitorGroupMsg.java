package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.APICreateAlarmMsg;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/monitorgroups",
        method = HttpMethod.POST,
        responseClass = APICreateMonitorGroupEvent.class,
        parameterName = "params"
)
public class APICreateMonitorGroupMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(required = false)
    private List<APICreateAlarmMsg.ActionParam> actions;

    public static APICreateMonitorGroupMsg __example__() {
        APICreateMonitorGroupMsg msg = new APICreateMonitorGroupMsg();
        msg.setName("vm-group-1");
        msg.setDescription("desc");
        APICreateAlarmMsg.ActionParam action = new APICreateAlarmMsg.ActionParam();
        action.actionType = SNSActionFactory.type.toString();
        action.actionUuid = uuid();
        msg.setActions(asList(action));
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateMonitorGroupEvent)rsp).getInventory().getUuid() : "", MonitorGroupVO.class);
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

    public List<APICreateAlarmMsg.ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<APICreateAlarmMsg.ActionParam> actions) {
        this.actions = actions;
    }
}
