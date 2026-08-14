package org.zstack.autoscaling.group.rule;

import org.springframework.http.HttpMethod;
import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by lining at 2018/9/11
 */
@RestRequest(
        path = "/autoscaling/rules/adding-new-instance",
        method = HttpMethod.POST,
        responseClass = APICreateAutoScalingRuleEvent.class,
        parameterName = "params"
)
public class APICreateAutoScalingGroupAddingNewInstanceRuleMsg extends APICreateAutoScalingGroupRuleMsg {
    @APIParam(validValues = {AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_QUANTITYCHANGEINCAPACITY,
            AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_PERCENTCHANGEINCAPACITY,
            AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_TOTALCAPACITY})
    private String adjustmentType;

    @APIParam(numberRange = {1, Long.MAX_VALUE})
    private Integer adjustmentValue;

    public static APICreateAutoScalingGroupAddingNewInstanceRuleMsg __example__() {
        APICreateAutoScalingGroupAddingNewInstanceRuleMsg msg = new APICreateAutoScalingGroupAddingNewInstanceRuleMsg();
        msg.setAutoScalingGroupUuid(uuid());
        msg.setName("addingNewInstance");
        msg.setDescription("just for test");
        msg.setCooldown(10l);
        msg.setAdjustmentType(AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_PERCENTCHANGEINCAPACITY);
        msg.setAdjustmentValue(1);
        return msg;
    }

    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateAutoScalingRuleEvent)rsp).getInventory().getUuid() : "", AutoScalingRuleVO.class);
    }

    @Override
    public String getType() {
        return AutoScalingConstants.AutoScalingRule.RULE_TYPE_ADDING_NEW_INSTANCE;
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
