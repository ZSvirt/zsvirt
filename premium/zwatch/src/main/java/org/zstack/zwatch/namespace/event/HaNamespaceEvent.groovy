package org.zstack.zwatch.namespace.event

import org.zstack.header.vm.VmTracerCanonicalEvents
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.HaNamespace

class HaNamespaceEvent {
    HaNamespaceEvent() {
        HaNamespace.MigrateVMFailedWithHostMaintain.onCanonicalEvent(VmTracerCanonicalEvents.MIGRATE_VM_FAILED_WITH_HOST_MAINTAIN_PATH) { VmTracerCanonicalEvents.MigrateVMFailedWithHostMaintainData data ->
            return new EventFamily.Event(data.vmUuid, data.hostUuid, data.vmUuid, data.reason)
        }
    }
}
