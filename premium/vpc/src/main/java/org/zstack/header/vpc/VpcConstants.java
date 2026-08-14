package org.zstack.header.vpc;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by weiwang on 18/09/2017
 */
@PythonClass
public class VpcConstants {
    @PythonClass
    public static final String VPC_VROUTER_VM_TYPE = "vpcvrouter";
    @PythonClass
    public static final String VPC_L3_NETWORK_TYPE = "L3VpcNetwork";
    @PythonClass
    public static final String SERVICE_ID = "vpc";
    public static final String ACTION_CATEGORY = "vpc";
    public static final String ANSIBLE_MODULE_PATH = "ansible/zsnagentansible";
    public static final String ANSIBLE_PLAYBOOK_NAME = "zsnagentansible.py";
    public static final String AGENT_PACKAGE_NAME = "zsn-agent.bin";

    public static final String VR_SET_VPCDNS_PATH = "/setvpcdns";
    public static final String VR_SET_VPC_NETWORK_SERVICE_SNAT_STATE_PATH = "/setsnatservicestate";

    public static final String VR_DR_STATE = "DistributedRoutingState";

    public static final String TC_FOR_VIPQOS = "ConfigTcForVipQos";
}