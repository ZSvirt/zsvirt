package org.zstack.ha.spi;

import org.zstack.ha.message.SelfFencerStruct;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHypervisorFactory {
    HaHypervisorWorker createHaWorker(VmInstanceInventory vm);

    SelfFencerHypervisorBackend createSelfFencerBackend(SelfFencerStruct struct);

    String getHypervisorType();
}
