package org.zstack.ha;

import org.zstack.header.vm.HaStartVmJudger;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;

/**
 * Created by xing5 on 2016/3/29.
 */
public class UnknownVmHaJudger implements HaStartVmJudger {
    @Override
    public boolean whetherStartVm(VmInstanceInventory vm) {
        return VmInstanceState.Unknown.toString().equals(vm.getState());
    }
}
