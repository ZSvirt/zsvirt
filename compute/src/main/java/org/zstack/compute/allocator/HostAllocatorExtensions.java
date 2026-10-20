package org.zstack.compute.allocator;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;

import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK;

public class HostAllocatorExtensions implements
        VmInstanceMigrateExtensionPoint
{
    @Autowired
    private EventFacade eventFacade;

    // ------------------------------------- VmInstanceMigrateExtensionPoint ------------------------------------

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        // Advance the time when the reservation takes effect,
        // so the event send time is moved from `beforeMigrateVm` to `preMigrateVm`
        // see ZSTAC-85091 and ZSV-13496
        fireMigrationHostCapacity(inv, destHostUuid, VmCanonicalEvents.VM_MIGRATION_HOST_CAPACITY_PATH);
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        fireMigrationHostCapacity(inv, inv.getHostUuid(), VmCanonicalEvents.VM_MIGRATION_HOST_CAPACITY_RELEASED_PATH);
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        fireMigrationHostCapacity(inv, destHostUuid, VmCanonicalEvents.VM_MIGRATION_HOST_CAPACITY_RELEASED_PATH);
    }

    private void fireMigrationHostCapacity(VmInstanceInventory inv, String hostUuid, String path) {
        if (hostUuid == null) {
            return;
        }
        VmCanonicalEvents.MigrationHostCapacityData data = new VmCanonicalEvents.MigrationHostCapacityData();
        data.setVmUuid(inv.getUuid());
        data.setHostUuid(hostUuid);
        data.setMemorySize(inv.getMemorySize());
        data.setCpuNum(inv.getCpuNum());
        String taskId = ThreadContext.get(THREAD_CONTEXT_TASK);
        data.setTaskId(taskId == null ? ThreadContext.get(THREAD_CONTEXT_API) : taskId);

        eventFacade.fire(path, data);
    }
}
