package org.zstack.storage.primary;

import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import static org.zstack.core.Platform.i18m;

public class PrimaryStorageReservedCapacityAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        for (PrimaryStorageCandidate candidate : candidates) {
            boolean enough = PrimaryStorageCapacityChecker.New(candidate.primaryStorage.getCapacity())
                    .checkIncreasedAndTotalRequiredSize(spec.getSize(), spec.getTotalSize());
            if (!enough) {
                reject(candidate, i18m("after subtracting reserved capacity[%s], available capacity cannot satisfy required size[%s bytes]",
                        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value(), spec.getSize()));
            }
        }
        next();
    }
}
