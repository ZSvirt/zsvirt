package org.zstack.allocator.domain.compute.flows;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.AllocatorDomainRuleEvaluator;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.host.HostVO;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionUtils.transform;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractDomainAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    protected DatabaseFacade databaseFacade;

    protected final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Override
    public final void allocate() {
        AllocatorDomainVO domain = loadDomainOrSkip();
        if (domain == null) {
            next();
            return;
        }
        allocate(domain);
    }

    protected abstract void allocate(AllocatorDomainVO domain);

    /**
     * @return {@code null} when there is no domain ruleSet (caller {@code next()}).
     * Missing domain fails 2001.
     */
    protected AllocatorDomainVO loadDomainOrSkip() {
        String domainUuid = domainUuidFromRuleSets(spec.getRuleSets());
        if (domainUuid == null) {
            return null;
        }
        AllocatorDomainVO domain = databaseFacade.findByUuid(domainUuid, AllocatorDomainVO.class);
        if (domain == null) {
            fail(err(AllocatorDomainErrors.DOMAIN_NOT_FOUND,
                    "allocator domain[uuid:%s] not found", domainUuid)
                    .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domainUuid));
            return null;
        }
        return domain;
    }

    protected String domainUuidFromRuleSets(List<AllocatorHostRuleSet> ruleSets) {
        if (ruleSets == null) {
            return null;
        }
        for (AllocatorHostRuleSet set : ruleSets) {
            if (set != null && AllocatorHostRuleSet.NAME_DOMAIN.equals(set.name)
                    && !StringUtils.isEmpty(set.domainUuid)) {
                return set.domainUuid;
            }
        }
        return null;
    }

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
