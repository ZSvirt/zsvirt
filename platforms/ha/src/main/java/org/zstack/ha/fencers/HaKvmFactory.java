package org.zstack.ha.fencers;

import org.zstack.ha.message.SelfFencerStruct;
import org.zstack.ha.spi.HaHypervisorFactory;
import org.zstack.ha.spi.HaHypervisorWorker;
import org.zstack.ha.spi.SelfFencerHypervisorBackend;
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
