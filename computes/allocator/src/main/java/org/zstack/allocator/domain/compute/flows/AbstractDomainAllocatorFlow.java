package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainRuleEvaluator;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.host.HostVO;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.transform;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractDomainAllocatorFlow extends AbstractHostAllocatorFlow {
    protected final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Override
    public final void allocate() {
        List<AllocatorHostRuleSet> ruleSets = spec.getRuleSets();
        if (ruleSets != null) {
            for (AllocatorHostRuleSet set : ruleSets) {
                if (set == null) {
                    continue;
                }
                allocate(set);
            }
        }
        next();
    }

    /**
     * Evaluate this set's rules together. Sets are AND-ed in list order by the caller.
     * Do not call {@link #next()}; the parent does that once after all sets.
     */
    protected abstract void allocate(AllocatorHostRuleSet domain);

    protected List<HostVO> candidateHosts() {
        return transform(candidates, c -> c.host);
    }

    protected void rejectHostsNotInClusters(Set<String> clusterUuids, I18nMessage reason) {
        Set<String> clusters = clusterUuids == null ? Collections.emptySet() : clusterUuids;
        for (HostCandidate candidate : candidates) {
            String clusterUuid = candidate.host.getClusterUuid();
            if (clusterUuid != null && clusters.contains(clusterUuid)) {
                continue;
            }
            reject(candidate, reason);
        }
    }
}
