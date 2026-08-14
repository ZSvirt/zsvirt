package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.l2.L2NetworkConstant;

import java.util.Arrays;
import java.util.List;

@PythonClass
public interface VmMacVlanNicConstant {
    String MACVLAN_NIC_TYPE = "MACVLAN";

    List<String> MACVLAN_L2_NETWORK_TYPES = Arrays.asList(
            L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE,
            L2NetworkConstant.L2_VLAN_NETWORK_TYPE
    );
}
