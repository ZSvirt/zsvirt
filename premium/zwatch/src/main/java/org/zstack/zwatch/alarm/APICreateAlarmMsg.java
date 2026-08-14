package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.SDK;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/zwatch/alarms",
        method = HttpMethod.POST,
        responseClass = APICreateAlarmEvent.class,
        parameterName = "params"
)
public class APICreateAlarmMsg extends APICreateMessage implements APIAuditor {
    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateAlarmEvent)rsp).getInventory().getUuid() : "", AlarmVO.class);
    }

    @SDK
    public static class ActionParam {
        public String actionUuid;
        public String actionType;
    }

    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(validValues = {"GreaterThanOrEqualTo", "GreaterThan", "LessThan", "LessThanOrEqualTo"})
    private String comparisonOperator;
    @APIParam(numberRange = {1, Integer.MAX_VALUE}, required = false)
    private Integer period;
    @APIParam
    private String namespace;
    @APIParam
    private String metricName;
    @APIParam(numberRange = {0, Long.MAX_VALUE})
    private Double threshold;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Integer repeatInterval;
    private List<Label> labels;
    private List<ActionParam> actions;

    @APIParam(numberRange = {-1, Integer.MAX_VALUE}, required = false)
    private Integer repeatCount = -1;

    @APIParam(required = false)
    private String type = AlarmType.Any.toString();

    @APIParam(required = false)
    private Boolean enableRecovery = false;

    @APIParam(required = false, validValues = {"Emergent", "Important", "Normal"})
    private String emergencyLevel = "Important";

    public static APICreateAlarmMsg __example__() {
        APICreateAlarmMsg ret = new APICreateAlarmMsg();
        ret.name = "VM CPU Alarm";
        ret.comparisonOperator = ComparisonOperator.LessThanOrEqualTo.toString();
        ret.period = 60;
        ret.namespace = "ZStack/VM";
        ret.metricName = VmNamespace.CPUIdleUtilization.getName();
        ret.threshold = 60D;
        ret.enableRecovery = true;
        ret.repeatInterval = (int) TimeUnit.MINUTES.toSeconds(30);
        ret.labels = asList(new Label(VmNamespace.LabelNames.VMUuid.toString(), uuid(VmInstanceVO.class)));
        ActionParam action = new ActionParam();
        action.actionType = SNSActionFactory.type.toString();
        action.actionUuid = uuid(AlarmActionVO.class);
        ret.actions = asList(action);
        ret.setType(AlarmType.Any.toString());
        return ret;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval) {
        this.repeatInterval = repeatInterval;
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

    public ComparisonOperator getComparisonOperator() {
        return ComparisonOperator.valueOf(comparisonOperator);
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator.toString();
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public List<ActionParam> getActions() {
        return actions;
    }

    public void setActions(List<ActionParam> actions) {
        this.actions = actions;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public boolean isEnableRecovery() {
        return enableRecovery;
    }

    public void setEnableRecovery(boolean enableRecovery) {
        this.enableRecovery = enableRecovery;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
}
