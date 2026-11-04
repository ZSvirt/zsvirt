package org.zstack.storage.primary;

import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.isEmpty;

public class PrimaryStorageAvoidAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        if (isEmpty(spec.getAvoidPrimaryStorageUuids())) {
            next();
            return;
        }

        for (PrimaryStorageCandidate candidate : candidates) {
            if (spec.getAvoidPrimaryStorageUuids().contains(candidate.getUuid())) {
                reject(candidate, i18m("in avoid primary storage list%s", spec.getAvoidPrimaryStorageUuids()));
            }
        }
        next();
    }
}
