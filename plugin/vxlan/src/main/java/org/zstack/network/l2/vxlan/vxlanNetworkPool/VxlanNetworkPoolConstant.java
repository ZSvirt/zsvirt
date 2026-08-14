package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by weiwang on 03/03/2017.
 */
@PythonClass
public class VxlanNetworkPoolConstant {
    @PythonClass
    public static final String VXLAN_NETWORK_POOL_TYPE = "VxlanNetworkPool";
    @PythonClass
    public static final String RANDOM_VNI_ALLOCATOR_STRATEGY = "RandomVniAllocatorStrategy";
    @PythonClass
    public static final Integer VXLAN_PORT = 8472;
    @PythonClass
    public static final String KVM_VXLAN_TYPE = "KVM_HOST_VXLAN";

    public static final String ACTION_CATEGORY = "vxlan";

    public static final String VXLAN_KVM_CHECK_L2VXLAN_NETWORK_PATH = "/network/l2vxlan/checkcidr";
    public static final String VXLAN_KVM_REALIZE_L2VXLAN_NETWORK_PATH = "/network/l2vxlan/createbridge";
    public static final String VXLAN_KVM_REALIZE_L2VXLAN_NETWORKS_PATH = "/network/l2vxlan/createbridges";
    public static final String VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORK_PATH = "/network/l2vxlan/populatefdb";
    public static final String VXLAN_KVM_POPULATE_FDB_L2VXLAN_NETWORKS_PATH = "/network/l2vxlan/populatefdbs";
    public static final String VXLAN_KVM_DELETE_FDB_L2VXLAN_NETWORKS_PATH = "/network/l2vxlan/deletefdbs";
    public static final String VXLAN_KVM_DELETE_L2VXLAN_NETWORK_PATH = "/network/l2vxlan/deletebridge";
}
