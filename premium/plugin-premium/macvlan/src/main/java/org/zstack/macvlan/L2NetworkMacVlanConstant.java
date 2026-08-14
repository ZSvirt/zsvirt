package org.zstack.macvlan;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface L2NetworkMacVlanConstant {
    public static final String HOSTNETWORK_MODE_MACVLAN_VEPA = "vepa";

    String KVM_CHECK_L2VLAN_NETWORK_MACVLAN_PATH = "/network/l2vlan/macvlan/checkbridge";

    String KVM_REALIZE_L2VLAN_NETWORK_MACVLAN_PATH = "/network/l2vlan/macvlan/createbridge";

    String KVM_DELETE_L2VLAN_NETWORK_MACVLAN_PATH = "/network/l2vlan/macvlan/deletebridge";
}
