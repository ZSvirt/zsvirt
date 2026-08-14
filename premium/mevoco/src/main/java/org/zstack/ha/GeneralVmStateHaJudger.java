package org.zstack.ha;

import org.zstack.header.vm.HaStartVmJudger;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;

public class GeneralVmStateHaJudger implements HaStartVmJudger {
    @Override
    public boolean whetherStartVm(VmInstanceInventory vm) {
        return VmInstanceState.Unknown.toString().equals(vm.getState())
                || VmInstanceState.Stopped.toString().equals(vm.getState());
    }
}
