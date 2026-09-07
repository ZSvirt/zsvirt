package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;

import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedL2NetworkAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        if (spec.getL3NetworkUuids().isEmpty()) {
            if (spec.isAllowNoL3Networks()) {
                next();
                return;
            }

            String clusterUuid = spec.getVmInstance().getClusterUuid();
            for (HostCandidate candidate : candidates) {
                if (!clusterUuid.equals(candidate.host.getClusterUuid())) {
                    reject(candidate, i18m("not in cluster[uuid:%s]", clusterUuid));
                }
            }
            next();
            return;
        }

        final AttachedL2NetworkHostMatcher matcher = new AttachedL2NetworkHostMatcher();
        List<String> l3Uuids = matcher.collectL3NetworkUuids(spec);
        Set<String> allowedHostUuids = matcher.listAllowedHostUuids(l3Uuids);
        for (HostCandidate candidate : candidates) {
            if (!allowedHostUuids.contains(candidate.getUuid())) {
                reject(candidate, i18m("not attached to L2Networks of L3Networks%s", spec.getL3NetworkUuids()));
            }
        }
        next();
    }
}
