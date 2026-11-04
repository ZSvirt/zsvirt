package org.zstack.storage.primary.allocate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageAllocatorFilterExtensionPoint;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageFilterFlow extends AbstractPrimaryStorageAllocatorFlow {
    private final CLogger logger = Utils.getLogger(PrimaryStorageFilterFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void allocate() {
        final ArrayList<PrimaryStorageCandidate> candidates = new ArrayList<>(this.candidates);

        for (PrimaryStorageAllocatorFilterExtensionPoint filter : pluginRgty.getExtensionList(PrimaryStorageAllocatorFilterExtensionPoint.class)) {
            int beforeCount = candidates.size();

            try {
                filter.filter(new ArrayList<>(candidates), spec);
            } catch (OperationFailureException e) {
                candidates.forEach(candidate -> candidate.markAsRejected(filter.getClass(), e.getErrorCode()));
                next();
                return;
            }

            for (Iterator<PrimaryStorageCandidate> iterator = candidates.iterator(); iterator.hasNext(); ) {
                PrimaryStorageCandidate candidate = iterator.next();
                if (candidate.reject != null) {
                    logger.debug(String.format("primary storage[%s] is rejected by %s: %s",
                            candidate.getUuid(), candidate.rejectBy, candidate.reject));
                    iterator.remove();
                }
            }

            logger.debug(String.format("after PrimaryStorageAllocatorFilterExtensionPoint[%s], candidates num: %s -> %s",
                    filter.getClass().getSimpleName(), beforeCount, candidates.size()));

            if (candidates.isEmpty()) {
                next();
                return;
            }
        }

        next();
    }
}
