package org.zstack.storage.backup;

import org.zstack.header.storage.backup.VolumeBackupFactory;
import org.zstack.header.storage.backup.VolumeBackupHypervisorBackend;
import org.zstack.kvm.KVMConstant;

public class VolumeBackupKvmFactory implements VolumeBackupFactory {
    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public VolumeBackupHypervisorBackend getHypervisorBackend() {
        return new VolumeBackupKvmBackend();
    }
}
