package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateVO;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.List;

@RestRequest(
        path = "/zwatch/monitortemplates/evenrules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateEventRuleTemplateEvent.class,
        isAction = true
)
public class APIUpdateEventRuleTemplateMsg extends APIMessage {
    @APIParam(resourceType = EventRuleTemplateVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel = "Important";

    @APIParam(required = false)
    private List<Label> labels;

    public static APIUpdateEventRuleTemplateMsg __example__() {
        APIUpdateEventRuleTemplateMsg msg = new APIUpdateEventRuleTemplateMsg();
        msg.setUuid(uuid());
        msg.setName(VmNamespace.VMStateChanged.getName());
        msg.setEmergencyLevel(EmergencyLevel.Normal.name());
        return msg;
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
