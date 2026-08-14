package org.zstack.vpc;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class VpcGlobalProperty {
    @GlobalProperty(name="zsn.agentPort", defaultValue = "7274")
    public static int AGENT_PORT;

    @GlobalProperty(name="upgradeVpcNetworkService", defaultValue = "false")
    public static boolean UPGRADE_VPC_NETWORK_SERVICE;

    @GlobalProperty(name="upgradeVpcNetworkServiceUnitTest", defaultValue = "false")
    public static boolean UPGRADE_VPC_NETWORK_SERVICE_UT;

    @GlobalProperty(name="upgradeVrToVpc", defaultValue = "false")
    public static boolean UPGRADE_VR_TO_VPC;

    @GlobalProperty(name="upgradeSystemVipNetworkServicesRefVO", defaultValue = "false")
    public static boolean UPGRADE_SYSTEM_VIP_SERVICE_REF;

    @GlobalProperty(name="upgradeVpcHaL3NetworkCheck", defaultValue = "false")
    public static boolean UPGRADE_VPC_HA_L3NETWORK_CHECK;

    @GlobalProperty(name="upgradeVipOwner", defaultValue = "false")
    public static boolean UPGRADE_VIP_OWNER;

    @GlobalProperty(name="upgradeVpcIpsecVersionCheck", defaultValue = "false")
    public static boolean UPGRADE_VPC_IPSEC_VERSION_CHECK;

    @GlobalProperty(name="upgradeVipSnatNetworkServiceRefRecord", defaultValue = "false")
    public static boolean UPGRADE_VIP_SNAT_NETWORK_SERVICE_REF_RECORD;
}
