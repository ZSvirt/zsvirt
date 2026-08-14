package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/events/subscriptions",
        method = HttpMethod.POST,
        responseClass = APISubscribeEventEvent.class,
        parameterName = "params")
public class APISubscribeEventMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam
    private String namespace;
    @APIParam
    private String eventName;
    @APIParam(required = false)
    private List<APICreateAlarmMsg.ActionParam> actions;
    private List<Label> labels;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel = "Important";

    public static APISubscribeEventMsg __example__() {
        APISubscribeEventMsg ret = new APISubscribeEventMsg();
        ret.setNamespace("ZStack/VM");
        ret.setEventName(VmNamespace.LabelNames.VMUuid.toString());
        APICreateAlarmMsg.ActionParam action = new APICreateAlarmMsg.ActionParam();
        action.actionType = SNSActionFactory.type.toString();
        action.actionUuid = uuid(AlarmActionVO.class);
        ret.actions = asList(action);
        ret.labels = Arrays.asList(new Label(VmNamespace.LabelNames.VMUuid.toString(), uuid(VmInstanceVO.class)));
        return ret;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public List<APICreateAlarmMsg.ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<APICreateAlarmMsg.ActionParam> actions) {
        this.actions = actions;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APISubscribeEventEvent)rsp).getInventory().getUuid() : "", EventSubscriptionVO.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
}
