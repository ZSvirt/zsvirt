package org.zstack.header.vm;

import org.springframework.lang.NonNull;

import java.util.List;

public interface VmMigrateHostConstraintExtensionPoint {
    /**
     * Returns candidate VMs pinned to their host by passthrough devices.
     * Migratable VF/vDPA NICs must not be treated as host constraints.
     */
    List<String> getHostBoundVmUuids(@NonNull List<String> candidateVmUuids);
}
