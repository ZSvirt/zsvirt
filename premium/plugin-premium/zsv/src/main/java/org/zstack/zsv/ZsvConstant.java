package org.zstack.zsv;

import org.zstack.header.configuration.PythonClass;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:47 2023/7/31
 */
@PythonClass
public class ZsvConstant {
    public static final String SERVICE_ID = "zsv";

    public static final Integer ZSV_DISASTER_RECOVERY_FREE_NUM = 6;

    public static final String KVM_AGENT_PID_PATH = "/run/zstack/kvmagent.pid";
    public static final String NODE_ROLE_MANAGEMENT = "management";
    public static final String NODE_ROLE_COMPUTE = "compute";

    public static final String CEPH_PLUGIN_PROPERTY_KEY_VERSION = "version";
}
