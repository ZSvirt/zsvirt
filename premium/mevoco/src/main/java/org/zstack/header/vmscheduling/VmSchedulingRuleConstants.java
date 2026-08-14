package org.zstack.header.vmscheduling;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
public class VmSchedulingRuleConstants {
    public static final String SERVICE_ID = "vmSchedulingRule";
    public static final String VM_SCHEDULING_RULE_GROUP_APPLIANCE = "CUSTOMER";
    public static final String VM_SCHEDULING_RULE_GROUP_RESERVE_ID = "VmSchedulingRuleGroupReserveFlow";
    public static final String CREATE_VM_GROUP_SRC_AFFINITYGROUP = "%s-src-affinityGroup";
    public static final String VM_SCHEDULING_RULE_TYPE = "vmSchedulingRule";
    public enum Param {
        RESERVE_SUCCESS,
        ORIGIN_HOST_UUID
    }
}
