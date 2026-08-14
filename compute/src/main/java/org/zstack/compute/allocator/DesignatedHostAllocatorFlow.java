package org.zstack.compute.allocator;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DesignatedHostAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(DesignatedHostAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

    private void allocate(List<HostCandidate> candidates,
                          String zoneUuid,
                          List<String> clusterUuids,
                          String hostUuid,
                          String hypervisorType) {
        for (HostCandidate candidate : candidates) {
            if (zoneUuid != null && !candidate.host.getZoneUuid().equals(zoneUuid)) {
                reject(candidate, i18m("not in zone[uuid:%s]", zoneUuid));
                continue;
            }
            if (!CollectionUtils.isEmpty(clusterUuids) && !clusterUuids.contains(candidate.host.getClusterUuid())) {
                reject(candidate, i18m("not in cluster[uuid:%s]", clusterUuids));
                continue;
            }
            if (hostUuid != null && !candidate.getUuid().equals(hostUuid)) {
                reject(candidate, i18m("must be host[uuid:%s]", hostUuid));
                continue;
            }
            if (hypervisorType != null && !candidate.host.getHypervisorType().equals(hypervisorType)) {
                reject(candidate, i18m("not with type[%s]", hypervisorType));
            }
        }
    }

    @Override
    public void allocate() {
        String zoneUuid = spec.getZoneUuid();
        List<String> clusterUuids = spec.getClusterUuids();
        String hostUuid = spec.getHostUuid();

        if (zoneUuid == null && CollectionUtils.isEmpty(clusterUuids) && hostUuid == null && spec.getHypervisorType() == null) {
            next();
            return;
        }

        allocate(candidates, zoneUuid, clusterUuids, hostUuid, spec.getHypervisorType());
        next();
    }
}
