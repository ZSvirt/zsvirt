package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.APICreateAlarmMsg;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;
import org.zstack.zwatch.monitorgroup.msg.MonitorGroupMsg;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/monitorgroups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateMonitorGroupEvent.class,
        isAction = true
)
public class APIUpdateMonitorGroupMsg extends APIMessage implements APIAuditor, MonitorGroupMsg {
    @APIParam(resourceType = MonitorGroupVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam( maxLength = 2048, required = false)
    private String description;

    @APIParam(required = false)
    private List<APICreateAlarmMsg.ActionParam> actions;

    @APIParam(validValues = {"enable","disable"}, required = false)
    private String stateEvent;

    @Override
    public String getMonitorGroupUuid() {
        return uuid;
    }

    public static APIUpdateMonitorGroupMsg __example__() {
        APIUpdateMonitorGroupMsg msg = new APIUpdateMonitorGroupMsg();
        msg.setUuid(uuid());
        msg.setName("group2");
        msg.setDescription("desc");
        msg.setStateEvent("enable");
        APICreateAlarmMsg.ActionParam action = new APICreateAlarmMsg.ActionParam();
        action.actionType = SNSActionFactory.type.toString();
        action.actionUuid = uuid();
        msg.setActions(asList(action));
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(rsp.isSuccess() ? ((APIUpdateMonitorGroupMsg)msg).getUuid() : "", MonitorGroupVO.class);
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

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    public List<APICreateAlarmMsg.ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<APICreateAlarmMsg.ActionParam> actions) {
        this.actions = actions;
    }
}
