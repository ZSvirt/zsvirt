package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.DRSConstants;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/12/12.
 */
@TagResourceType(ClusterDRSVO.class)
@RestRequest(
        path = "/clusters/{clusterUuid}/drs",
        method = HttpMethod.POST,
        responseClass = APICreateClusterDRSEvent.class,
        parameterName = "params"
)
public class APICreateClusterDRSMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(resourceType = ClusterVO.class, maxLength = 32)
    private String clusterUuid;

    @APIParam(validValues = {DRSConstants.AUTOMATION_LEVEL_AUTOMATIC, DRSConstants.AUTOMATION_LEVEL_MANUAL})
    private String automationLevel;

    @APIParam
    private List<Threshold> thresholds;

    @APIParam
    private Integer thresholdDuration;

    @APIParam(required = false)
    private boolean defaultEnable;

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
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

    public boolean isDefaultEnable() {
        return defaultEnable;
    }

    public void setDefaultEnable(boolean defaultEnable) {
        this.defaultEnable = defaultEnable;
    }

    public static APICreateClusterDRSMsg __example__() {
        APICreateClusterDRSMsg msg = new APICreateClusterDRSMsg();
        msg.setName("test");
        msg.setClusterUuid(uuid(ClusterVO.class));
        msg.setAutomationLevel(DRSConstants.AUTOMATION_LEVEL_AUTOMATIC);
        msg.setThresholdDuration(10);
        msg.setThresholds(list(Threshold.__example__()));
        msg.setDefaultEnable(true);
        return msg;
    }
}
