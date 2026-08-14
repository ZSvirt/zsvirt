package org.zstack.mevoco;

import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.header.vm.VmInstanceVO;

public interface PremiumVmInstanceFactory {
    VmInstanceType getType();

    VmInstance getVmInstance(VmInstanceVO vo);
}
