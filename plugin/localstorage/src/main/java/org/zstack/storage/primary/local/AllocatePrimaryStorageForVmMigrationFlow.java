package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.i18m;

/**
 * Created by frank on 10/24/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AllocatePrimaryStorageForVmMigrationFlow  extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Override
    public void allocate() {
        if (!VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            throw new CloudRuntimeException("AllocatePrimaryStorageForVmMigrationFlow is only used for migrating vm");
        }

        String psUuid = spec.getVmInstance().getRootVolume().getPrimaryStorageUuid();
        List<String> huuids = allHostUuidList();

        long volumeSize = 0;
        List<String> volUuids = new ArrayList<>();
        for (VolumeInventory vol : spec.getVmInstance().getAllVolumes()) {
            volumeSize += vol.getSize();
            volUuids.add(vol.getUuid());
        }

        long snapshotSize = 0;
        List<Long> snapshotSizes = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.size)
                .in(VolumeSnapshotVO_.volumeUuid, volUuids)
                .listValues();
        for (Long s : snapshotSizes) {
            snapshotSize += s;
        }

        List<LocalStorageHostRefVO> refs = Q.New(LocalStorageHostRefVO.class)
                .in(LocalStorageHostRefVO_.hostUuid, huuids)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, psUuid)
                .list();

        final List<String> hostUuids = new ArrayList<>();
        for (LocalStorageHostRefVO ref : refs) {
            if (ref.getAvailableCapacity() > ratioMgr.calculateByRatio(psUuid, volumeSize) + snapshotSize) {
                hostUuids.add(ref.getHostUuid());
            }
        }

        for (HostCandidate candidate : candidates) {
            if (!hostUuids.contains(candidate.getUuid())) {
                reject(candidate, i18m("not enough space on host"));
            }
        }

        next();
    }
}
