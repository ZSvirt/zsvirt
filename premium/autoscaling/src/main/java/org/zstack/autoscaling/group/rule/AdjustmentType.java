package org.zstack.autoscaling.group.rule;

import org.zstack.autoscaling.AutoScalingConstants;

/**
 * Created by lining on 2018/9/4.
 */
public enum AdjustmentType {
    QuantityChangeInCapacity(AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_QUANTITYCHANGEINCAPACITY),
    PercentChangeInCapacity(AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_PERCENTCHANGEINCAPACITY),
    TotalCapacity(AutoScalingConstants.AutoScalingRule.ADJUSTMENTTYPE_TOTALCAPACITY);

    private String name;

    AdjustmentType(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
