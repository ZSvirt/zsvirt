package org.zstack.autoscaling.group.rule;

/**
 * Created by lining on 2018/9/21.
 */
public class AutoScalingRuleAdjustmentUtils {
    public static int getValue(AdjustmentType type, int adjustmentValue, int existingQuantity) {
        if (AdjustmentType.QuantityChangeInCapacity == type) {
            return adjustmentValue;
        } else if (AdjustmentType.PercentChangeInCapacity == type) {
            int value = existingQuantity * (adjustmentValue / 100);
            value = value == 0 ? 1 : value;
            return value;
        } else if (AdjustmentType.TotalCapacity == type) {
            int value = existingQuantity - adjustmentValue;
            value = value > 0 ? value : 0;
            return value;
        } else {
            //lining123
            return 0;
        }
    }
}
