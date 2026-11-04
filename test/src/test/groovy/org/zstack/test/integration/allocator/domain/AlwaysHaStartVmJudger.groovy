package org.zstack.test.integration.allocator.domain

import org.zstack.header.vm.HaStartVmJudger
import org.zstack.header.vm.VmInstanceInventory

class AlwaysHaStartVmJudger implements HaStartVmJudger {
    @Override
    boolean whetherStartVm(VmInstanceInventory vm) {
        return true
    }
}
