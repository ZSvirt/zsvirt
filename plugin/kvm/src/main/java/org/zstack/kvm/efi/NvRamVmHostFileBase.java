package org.zstack.kvm.efi;

import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.kvm.vmfiles.AbstractVmHostFileBase;

public class NvRamVmHostFileBase extends AbstractVmHostFileBase {
    public NvRamVmHostFileBase(VmHostFileVO self) {
        super(self);
    }

    @Override
    public VmHostFileType type() {
        return VmHostFileType.NvRam;
    }
}
