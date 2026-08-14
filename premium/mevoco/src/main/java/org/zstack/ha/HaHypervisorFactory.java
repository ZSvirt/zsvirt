package org.zstack.ha;

import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHypervisorFactory {
    HaHypervisorWorker createHaWorker(VmInstanceInventory vm);

    SelfFencerHypervisorBackend createSelfFencerBackend(SelfFencerStruct struct);

    String getHypervisorType();
}
