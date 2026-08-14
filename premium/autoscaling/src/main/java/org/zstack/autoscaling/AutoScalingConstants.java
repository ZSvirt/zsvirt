package org.zstack.autoscaling;

/**
 * Create by weiwang at 2018/8/17
 */
public class AutoScalingConstants {
    public static final String SERVICE_ID = "autoscaling";

    public interface AutoScalingTemplate {
        interface VmInstance  {
            String TYPE = "VmInstance";
            String SEPARATOR = ",";
        }
    }

    public static final int ACTIVITY_RETENTION_BUFFER = 100;
    public static final String REMOVAL_POLICY_OLDEST_INSTANCE = "OldestInstance";
    public static final String REMOVAL_POLICY_NEWEST_INSTANCE = "NewestInstance";
    public static final String REMOVAL_POLICY_OLDEST_SCALING_CONFIGURATION = "OldestScalingConfiguration";
    public static final String REMOVAL_POLICY_MINIMUM_CPU_USAGE_INSTANCE = "MinimumCPUUsageInstance";
    public static final String REMOVAL_POLICY_MINIMUM_MEMORY_USAGE_INSTANCE = "MinimumMemoryUsageInstance";

    public interface AutoScalingRule {
        String ADJUSTMENTTYPE_QUANTITYCHANGEINCAPACITY = "QuantityChangeInCapacity";
        String ADJUSTMENTTYPE_PERCENTCHANGEINCAPACITY = "PercentChangeInCapacity";
        String ADJUSTMENTTYPE_TOTALCAPACITY = "TotalCapacity";

        String RULE_TYPE_ADDING_NEW_INSTANCE = "AddingNewInstanceRule";
        String RULE_TYPE_REMOVAL_INSTANCE = "RemovalInstanceRule";

        interface TriggerType {
            String Alarm = "Alarm";
            String TimedTask = "TimedTask";
        }
    }

    public interface AutoScalingGroupInstance {
        String PROTECTION_STRATEGY_UNPROTECTED = "Unprotected";
        String PROTECTION_STRATEGY_PROTECTED = "Protected";
    }
}
