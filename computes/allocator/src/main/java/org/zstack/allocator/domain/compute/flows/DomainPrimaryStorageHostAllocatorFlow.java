package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainRuleEvaluator;
import org.zstack.header.allocator.domain.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainPrimaryStorageHostAllocatorFlow extends AbstractDomainAllocatorFlow {
    @Override
    protected void allocate(AllocatorHostRuleSet domain) {
        List<String> zonePrimaryStorages = listZonePrimaryStorageUuids(domain.zoneUuid);
        AllocatorDomainRuleEvaluator.UuidDecision psDecision = evaluator.evaluatePrimaryStorages(
                domain.rules, zonePrimaryStorages);
        if (!psDecision.hasChain) {
            return;
        }

        Set<String> required = spec.getRequiredPrimaryStorageUuids() == null
                ? Collections.emptySet() : spec.getRequiredPrimaryStorageUuids();
        for (String psUuid : required) {
            if (!psDecision.allowedUuids.contains(psUuid)) {
                fail(err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                        "primary storage[uuid:%s] is not in allocator domain[uuid:%s]", psUuid, domain.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domain.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, psUuid));
                return;
            }
        }

        Set<String> allowedClusters = required.isEmpty()
                ? clustersAttachedToAny(psDecision.allowedUuids)
                : clustersAttachedToAllPrimaryStorages(required);
        rejectHostsNotInClusters(allowedClusters,
                i18m("cluster not attached to allocator domain[uuid:%s] primary storage", domain.domainUuid));
    }

    private List<String> listZonePrimaryStorageUuids(String zoneUuid) {
        List<String> uuids = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                .select(PrimaryStorageVO_.uuid)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    private Set<String> clustersAttachedToAllPrimaryStorages(Collection<String> psUuids) {
        Set<String> clusters = null;
        for (String psUuid : psUuids) {
            List<String> attached = listClusterUuidsByPrimaryStorage(psUuid);
            if (clusters == null) {
                clusters = new LinkedHashSet<>(attached);
            } else {
                clusters.retainAll(attached);
            }
        }
        return clusters == null ? Collections.emptySet() : clusters;
    }

    private Set<String> clustersAttachedToAny(Collection<String> psUuids) {
        if (psUuids == null || psUuids.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> values = Q.New(PrimaryStorageClusterRefVO.class)
                .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuids)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .listValues();
        return values == null ? Collections.emptySet() : new LinkedHashSet<>(values);
    }

    private List<String> listClusterUuidsByPrimaryStorage(String psUuid) {
        List<String> values = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .listValues();
        return values == null ? new ArrayList<>() : values;
    }
}
