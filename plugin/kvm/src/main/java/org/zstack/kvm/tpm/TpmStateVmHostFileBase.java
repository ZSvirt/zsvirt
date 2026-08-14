package org.zstack.kvm.tpm;

import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.kvm.vmfiles.AbstractVmHostFileBase;

public class TpmStateVmHostFileBase extends AbstractVmHostFileBase {
    public TpmStateVmHostFileBase(VmHostFileVO self) {
        super(self);
    }

    @Override
    public VmHostFileType type() {
        return VmHostFileType.TpmState;
    }
}
