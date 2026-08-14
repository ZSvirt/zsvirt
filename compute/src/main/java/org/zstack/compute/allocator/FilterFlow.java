package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by frank on 7/2/2015.
 */
public class FilterFlow extends AbstractHostAllocatorFlow {
    private final CLogger logger = Utils.getLogger(FilterFlow.class);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void allocate() {
        final ArrayList<HostCandidate> candidates = new ArrayList<>(this.candidates);

        for (HostAllocatorFilterExtensionPoint filter : pluginRgty.getExtensionList(HostAllocatorFilterExtensionPoint.class)) {
            int beforeCount = candidates.size();

            try {
                filter.filter(new ArrayList<>(candidates), spec);
            } catch (OperationFailureException e) {
                candidates.forEach(candidate -> candidate.markAsRejected(filter.getClass(), e.getErrorCode()));
                next();
                return;
            }

            for (Iterator<HostCandidate> iterator = candidates.iterator(); iterator.hasNext();) {
                HostCandidate candidate = iterator.next();
                if (candidate.reject != null) {
                    logger.debug(String.format("host[%s] is rejected by %s: %s",
                            candidate.getUuid(), candidate.rejectBy, candidate.reject));
                    iterator.remove();
                }
            }

            logger.debug(String.format("after HostAllocatorFilterExtensionPoint[%s], candidates num: %s -> %s",
                    filter.getClass().getSimpleName(), beforeCount, candidates.size()));

            if (candidates.isEmpty()) {
                next();
                return;
            }
        }

        next();
    }
}
