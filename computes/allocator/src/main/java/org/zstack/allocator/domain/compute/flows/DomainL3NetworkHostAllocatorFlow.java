package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainRuleEvaluator;
import org.zstack.header.allocator.domain.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainL3NetworkHostAllocatorFlow extends AbstractDomainAllocatorFlow {
    @Override
    protected void allocate(AllocatorHostRuleSet domain) {
        List<String> zoneL3s = listZoneL3NetworkUuids(domain.zoneUuid);
        AllocatorDomainRuleEvaluator.UuidDecision l3Decision = evaluator.evaluateL3Networks(domain.rules, zoneL3s);
        if (!l3Decision.hasChain) {
            return;
        }

        List<String> specified = spec.getL3NetworkUuids();
        if (specified == null || specified.isEmpty()) {
            return;
        }

        for (String l3Uuid : specified) {
            if (!l3Decision.allowedUuids.contains(l3Uuid)) {
                fail(err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                        "l3 network[uuid:%s] is not in allocator domain[uuid:%s]", l3Uuid, domain.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domain.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.L3_UUID, l3Uuid));
                return;
            }
        }

        rejectHostsNotInClusters(clustersAttachedToAllL3Networks(specified),
                i18m("cluster not attached to specified l3 of allocator domain[uuid:%s]", domain.domainUuid));
    }

    private List<String> listZoneL3NetworkUuids(String zoneUuid) {
        List<String> uuids = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.zoneUuid, zoneUuid)
                .eq(L3NetworkVO_.state, L3NetworkState.Enabled)
                .select(L3NetworkVO_.uuid)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    private Set<String> clustersAttachedToAllL3Networks(Collection<String> l3Uuids) {
        Set<String> clusters = null;
        for (String l3Uuid : l3Uuids) {
            String l2Uuid = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .select(L3NetworkVO_.l2NetworkUuid)
                    .findValue();
            if (l2Uuid == null) {
                return Collections.emptySet();
            }
            List<String> attached = listClusterUuidsByL2(l2Uuid);
            if (clusters == null) {
                clusters = new LinkedHashSet<>(attached);
            } else {
                clusters.retainAll(attached);
            }
        }
        return clusters == null ? Collections.emptySet() : clusters;
    }

    private List<String> listClusterUuidsByL2(String l2Uuid) {
        List<String> values = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid)
                .select(L2NetworkClusterRefVO_.clusterUuid)
                .listValues();
        return values == null ? new ArrayList<>() : values;
    }
}
