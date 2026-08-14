package org.zstack.header.vmscheduling;

import org.zstack.header.affinitygroup.AffinityGroupPolicy;

public class VmSchedulingRulePolicy {
    public static AffinityGroupPolicy getAffinityGroupPolicy(String rule, String mode) {
        if (rule.equals(VMSchedulingRuleType.AFFINITY.toString()) && mode.equals(VMSchedulingRuleMode.HARD.toString())) {
            return AffinityGroupPolicy.AFFINITYHARD;
        } else if (rule.equals(VMSchedulingRuleType.AFFINITY.toString()) && mode.equals(VMSchedulingRuleMode.SOFT.toString())) {
            return AffinityGroupPolicy.AFFINITYSOFT;
        } else if (rule.equals(VMSchedulingRuleType.ANTIAFFINITY.toString()) && mode.equals(VMSchedulingRuleMode.SOFT.toString())) {
            return AffinityGroupPolicy.ANTISOFT;
        } else {
            return AffinityGroupPolicy.ANTIHARD;
        }
    }

    public static VMSchedulingRuleMode getVMSchedulingRuleLevel(AffinityGroupPolicy policy) {
        if (policy == AffinityGroupPolicy.AFFINITYSOFT || policy == AffinityGroupPolicy.ANTISOFT) {
            return VMSchedulingRuleMode.SOFT;
        }
        return VMSchedulingRuleMode.HARD;
    }

    public static VMSchedulingRuleType getVMSchedulingRuleType(AffinityGroupPolicy policy) {
        if (policy == AffinityGroupPolicy.AFFINITYSOFT || policy == AffinityGroupPolicy.AFFINITYHARD) {
            return VMSchedulingRuleType.AFFINITY;
        }
        return VMSchedulingRuleType.ANTIAFFINITY;
    }
}
