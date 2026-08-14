package org.zstack.ha;

import org.zstack.header.vm.HaStartVmJudger;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;

/**
 * Created by mingjian.deng on 2019/2/27.
 */
public class HaStartVirtualRouterVmJudger implements HaStartVmJudger {
    @Override
    public boolean whetherStartVm(VmInstanceInventory vm) {
        return (VmInstanceState.Stopped.toString().equals(vm.getState())
                || VmInstanceState.Unknown.toString().equals(vm.getState()));
    }
}
