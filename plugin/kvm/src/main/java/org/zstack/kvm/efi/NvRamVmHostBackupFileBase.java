package org.zstack.kvm.efi;

import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.kvm.vmfiles.AbstractVmHostBackupFileBase;

public class NvRamVmHostBackupFileBase extends AbstractVmHostBackupFileBase {
    public NvRamVmHostBackupFileBase(VmHostBackupFileVO self) {
        super(self);
    }

    @Override
    public VmHostFileType type() {
        return VmHostFileType.NvRam;
    }
}
