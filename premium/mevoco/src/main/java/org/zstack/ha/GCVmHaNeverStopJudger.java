package org.zstack.ha;

import org.zstack.core.db.Q;
import org.zstack.header.vm.HaStartVmJudger;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;

/**
 * Created by xing5 on 2016/4/8.
 */
public class GCVmHaNeverStopJudger implements HaStartVmJudger {
    @Override
    public boolean whetherStartVm(VmInstanceInventory vm) {
        VmHaLevel level = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vm.getUuid())
                .select(VmHaVO_.haLevel)
                .findValue();
        if (level != VmHaLevel.NeverStop) {
            return false;
        }

        return (VmInstanceState.Stopped.toString().equals(vm.getState())
                || VmInstanceState.Unknown.toString().equals(vm.getState()));
    }
}
