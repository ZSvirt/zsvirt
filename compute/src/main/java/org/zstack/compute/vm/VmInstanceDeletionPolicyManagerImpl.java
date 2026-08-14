package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceDeletionPolicyManager;

/**
 * Created by frank on 11/12/2015.
 */
public class VmInstanceDeletionPolicyManagerImpl implements VmInstanceDeletionPolicyManager {

    @Override
    public VmInstanceDeletionPolicy getDeletionPolicy(String vmUuid) {
        return VmInstanceDeletionPolicy.valueOf(VmGlobalConfig.VM_DELETION_POLICY.value());
    }
}
