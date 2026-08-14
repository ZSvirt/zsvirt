package org.zstack.storage.cbt;

import org.zstack.header.cbt.CbtBackupFactory;
import org.zstack.kvm.KVMConstant;

public class CbtBackupKvmFactory implements CbtBackupFactory {
    private final CbtBackupHypervisorBackend backend = new CbtBackupKvmBackend();

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public CbtBackupHypervisorBackend getHypervisorBackend() {
        return backend;
    }
}
