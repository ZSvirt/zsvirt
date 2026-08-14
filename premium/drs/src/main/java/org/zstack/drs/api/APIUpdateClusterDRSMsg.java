package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.DRSConstants;
import org.zstack.drs.DRSMessage;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */
@RestRequest(
        path = "/clusters/drs/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateClusterDRSEvent.class,
        isAction = true
)
public class APIUpdateClusterDRSMsg extends APIMessage implements DRSMessage {
    @APIParam(resourceType = ClusterDRSVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, validValues = {DRSConstants.AUTOMATION_LEVEL_AUTOMATIC, DRSConstants.AUTOMATION_LEVEL_MANUAL})
    private String automationLevel;

    @APIParam(required = false)
    private List<Threshold> thresholds;

    @APIParam(required = false)
    private Integer thresholdDuration;

    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;

    @Override
    public String getDRSUuid() {
        return uuid;
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

    public String getAutomationLevel() {
        return automationLevel;
    }

    public void setAutomationLevel(String automationLevel) {
        this.automationLevel = automationLevel;
    }

    public List<Threshold> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<Threshold> thresholds) {
        this.thresholds = thresholds;
    }

    public Integer getThresholdDuration() {
        return thresholdDuration;
    }

    public void setThresholdDuration(Integer thresholdDuration) {
        this.thresholdDuration = thresholdDuration;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIUpdateClusterDRSMsg __example__() {
        APIUpdateClusterDRSMsg msg = new APIUpdateClusterDRSMsg();
        msg.setUuid(uuid(ClusterDRSVO.class));
        msg.setName("Test");
        msg.setDescription("Test");
        msg.setAutomationLevel("Manual");
        msg.setThresholdDuration(10);
        msg.setState("Enabled");
        return msg;
    }
}
