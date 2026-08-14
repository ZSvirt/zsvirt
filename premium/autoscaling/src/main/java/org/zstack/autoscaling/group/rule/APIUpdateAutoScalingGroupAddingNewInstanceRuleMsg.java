package org.zstack.autoscaling.group.rule;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by lining at 2018/10/11
 */
@RestRequest(
        path = "/autoscaling/rules/adding-new-instance/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAutoScalingRuleEvent.class,
        isAction = true
)
public class APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg extends APIUpdateAutoScalingRuleMsg {
    @APIParam(validValues = {AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_QUANTITYCHANGEINCAPACITY,
            AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_PERCENTCHANGEINCAPACITY,
            AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_TOTALCAPACITY}, required = false)
    private String adjustmentType;

    @APIParam(numberRange = {1, Long.MAX_VALUE}, required = false)
    private Integer adjustmentValue;

    public static APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg __example__() {
        APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg msg = new APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg();
        msg.setUuid(uuid());
        msg.setName("test name2");
        return msg;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public Integer getAdjustmentValue() {
        return adjustmentValue;
    }

    public void setAdjustmentValue(Integer adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }
}
