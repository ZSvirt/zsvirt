package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import static org.zstack.core.Platform.i18m;

public class LocalStorageTypeAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        for (PrimaryStorageCandidate candidate : candidates) {
            PrimaryStorageVO vo = candidate.primaryStorage;
            if (!LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(vo.getType())) {
                reject(candidate, i18m("type %s is not local primary storage", vo.getType()));
            }
        }
        next();
    }
}
