package org.zstack.header.vHostUser;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.l2.L2NetworkConstant;

import java.util.Arrays;
import java.util.List;

@PythonClass
public interface VmVHostUserNicConstant {
    String SERVICE_ID = "vHostUser";

    List<String> VHOST_USER_SPACE_L2_NETWORK_TYPES = Arrays.asList(
            L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE,
            L2NetworkConstant.L2_VLAN_NETWORK_TYPE
    );

    List<String> VHOST_USER_SPACE_VSWITCH_TYPES = Arrays.asList(
            L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK,
            L2NetworkConstant.VSWITCH_TYPE_OVS_KERNEL
    );
}
