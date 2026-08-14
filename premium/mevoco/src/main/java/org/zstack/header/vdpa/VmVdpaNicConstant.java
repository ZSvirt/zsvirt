package org.zstack.header.vdpa;

import org.zstack.header.vm.VmOvsNicConstant;
import org.zstack.header.configuration.PythonClass;
import org.zstack.header.network.l2.L2NetworkConstant;

import java.util.Arrays;
import java.util.List;


/**
 * Created by haibiao.xiao on 3/23/2021.
 */
@PythonClass
public interface VmVdpaNicConstant {
    String SERVICE_ID = "vdpa";

    String VIRTIO_DATA_PATH_ACCEL_TYPE = VmOvsNicConstant.ACCEL_TYPE_VDPA;

    String ALLOCATE_VDPA_NIC_PCI_DEVICE = "allocate-vdpa-nic-pci-device";

    List<String> VDPA_L2_NETWORK_TYPES = Arrays.asList(
            L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE,
            L2NetworkConstant.L2_VLAN_NETWORK_TYPE
    );

    List<String> VDPA_VSWITCH_TYPES = Arrays.asList(
            L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK,
            L2NetworkConstant.VSWITCH_TYPE_OVS_KERNEL
    );

    double OVS_DPDK_MEM_USAGE_PRECENT = 0.85;

    List<String> SMART_NIC_DEPENDENCIES = Arrays.asList("kmod-kernel-mft-mlnx", "kmod-mlnx-ofa_kernel", "python3 openvswitch");
}
