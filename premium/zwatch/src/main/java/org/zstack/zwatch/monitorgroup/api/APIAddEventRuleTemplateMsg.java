package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.monitorgroup.entity.MonitorTemplateVO;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.List;

/**
 * Create by yaoning at 2020/10/10
 */
@RestRequest(
        path = "/zwatch/monitortemplates/evenrules",
        method = HttpMethod.POST,
        responseClass = APIAddEventRuleTemplateEvent.class,
        parameterName = "params"
)
public class APIAddEventRuleTemplateMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = MonitorTemplateVO.class)
    private String monitorTemplateUuid;

    @APIParam
    private String namespace;

    @APIParam
    private String eventName;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel = "Important";

    @APIParam(required = false)
    private List<Label> labels;

    public static APIAddEventRuleTemplateMsg __example__() {
        APIAddEventRuleTemplateMsg msg = new APIAddEventRuleTemplateMsg();
        msg.setMonitorTemplateUuid(uuid());
        msg.setName("VMStateChanged");
        msg.setNamespace("ZStack/VM");
        msg.setEventName(VmNamespace.VMStateChanged.getName());
        msg.setEmergencyLevel(EmergencyLevel.Normal.name());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddEventRuleTemplateMsg)msg).getMonitorTemplateUuid() : "", MonitorTemplateVO.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMonitorTemplateUuid() {
        return monitorTemplateUuid;
    }

    public void setMonitorTemplateUuid(String monitorTemplateUuid) {
        this.monitorTemplateUuid = monitorTemplateUuid;
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

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }
}
