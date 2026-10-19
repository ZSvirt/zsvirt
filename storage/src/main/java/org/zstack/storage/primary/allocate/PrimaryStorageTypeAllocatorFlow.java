package org.zstack.storage.primary.allocate;

import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.List;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.isEmpty;

public class PrimaryStorageTypeAllocatorFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        List<String> possibleTypes = spec.getPossiblePrimaryStorageTypes();
        List<String> excludeTypes = spec.getExcludePrimaryStorageTypes();
        boolean applyExclude = spec.getCandidatePrimaryStorageUuids().isEmpty() && !isEmpty(excludeTypes);

        for (PrimaryStorageCandidate candidate : candidates) {
            String type = candidate.primaryStorage.getType();
            if (!isEmpty(possibleTypes) && !possibleTypes.contains(type)) {
                reject(candidate, i18m("type %s is not in possible primary storage types%s", type, possibleTypes));
            } else if (applyExclude && excludeTypes.contains(type)) {
                reject(candidate, i18m("type %s is in exclude primary storage types%s", type, excludeTypes));
            }
        }
        next();
    }
}
