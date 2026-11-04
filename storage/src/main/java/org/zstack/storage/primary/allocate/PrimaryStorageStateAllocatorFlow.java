package org.zstack.storage.primary.allocate;

import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import static org.zstack.core.Platform.i18m;

public class PrimaryStorageStateAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        for (PrimaryStorageCandidate candidate : candidates) {
            PrimaryStorageVO vo = candidate.primaryStorage;
            if (vo.getState() != PrimaryStorageState.Enabled || vo.getStatus() != PrimaryStorageStatus.Connected) {
                reject(candidate, i18m("state is %s and status is %s, required state %s and status %s",
                        vo.getState(), vo.getStatus(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected));
            }
        }
        next();
    }
}
