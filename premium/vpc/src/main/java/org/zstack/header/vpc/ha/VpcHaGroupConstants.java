package org.zstack.header.vpc.ha;

import org.zstack.header.configuration.PythonClass;

public class VpcHaGroupConstants {
    public static final String SERVICE_ID = "vpcHa";
    public static final String ACTION_CATEGORY = "vpcHa";

    public static final String VPC_ROUTER_STATUS_REPORT = "/vpc/hastatus";

    @PythonClass
    public static final String VPCHA_GROUP_VROUTER_VM_TYPE = "vpcHaVRouter";

    public enum Params{
        VPCHA_INVENTORY,
        VIRTUAL_ROUTER_OFFERING,
        AFFINITYGROUP_INVENTORY,
        PUBLIC_VIP_INVENTORY,
        PUBLIC_L3_UUIDS,
        VPCHA_ROUTERVM_INVENTORY,
    }
}
