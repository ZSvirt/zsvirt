package org.zstack.ha.vmboot;

import org.zstack.core.db.Q;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.header.vm.HaStartVmJudger;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;

public class HaStartVmJudgers {
    public static class GeneralJudger implements HaStartVmJudger {
        @Override
        public boolean whetherStartVm(VmInstanceInventory vm) {
            return VmInstanceState.Unknown.toString().equals(vm.getState())
                    || VmInstanceState.Stopped.toString().equals(vm.getState());
        }
    }

    public static class UnknownVmJudger implements HaStartVmJudger {
        @Override
        public boolean whetherStartVm(VmInstanceInventory vm) {
            return VmInstanceState.Unknown.toString().equals(vm.getState());
        }
    }

    /**
     * use for GC
     */
    public static class NeverStopJudger implements HaStartVmJudger {
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
}
