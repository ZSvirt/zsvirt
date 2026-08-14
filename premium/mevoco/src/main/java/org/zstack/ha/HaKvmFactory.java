package org.zstack.ha;

import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMConstant;

/**
 * Created by xing5 on 2016/3/28.
 */
public class HaKvmFactory implements HaHypervisorFactory {
    @Override
    public HaHypervisorWorker createHaWorker(VmInstanceInventory vm) {
        return new HaKvmWorker(vm);
    }

    @Override
    public SelfFencerHypervisorBackend createSelfFencerBackend(SelfFencerStruct struct) {
        return new SelfFencerKvmBackend(struct);
    }

    @Override
    public String getHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }
}
