package org.zstack.pciDevice.virtual.vfio_mdev;

import org.jetbrains.annotations.NotNull;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmMigrateHostConstraintExtensionPoint;

import java.util.List;

public class MdevDeviceExtensions implements
        VmMigrateHostConstraintExtensionPoint {

    @Override
    public List<String> getHostBoundVmUuids(@NotNull List<String> candidateVmUuids) {
        if (candidateVmUuids.isEmpty()) {
            return candidateVmUuids;
        }

        return Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.vmInstanceUuid, candidateVmUuids)
                .select(MdevDeviceVO_.vmInstanceUuid)
                .listValues();
    }
}
