package org.zstack.header.sriov;

import org.zstack.header.configuration.PythonClass;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 11/28/19.
 */
@PythonClass
public interface VmVfNicConstant {
    String SERVICE_ID = "sriov";
    String ACTION_CATEGORY = "sriov";

    String VIRTUAL_FUNCTION_TYPE = "VF";
    String ALLOCATE_VF_NIC_PCI_DEVICE = "allocate-vf-nic-pci-device";

    enum Params {
        VmVfNicInventory,
        VmVfNicHaState,
    }
}
