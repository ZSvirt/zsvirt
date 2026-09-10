package org.zstack.allocator.domain.compute.flows;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.allocator.domain.compute.AllocatorDomainRuleEvaluator;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.i18m;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DomainHostAllocatorFlow extends AbstractDomainAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(DomainHostAllocatorFlow.class);

    @Override
    protected void allocate(AllocatorHostRuleSet domain) {
        if (!evaluator.hasHostChain(domain.rules)) {
            return;
        }

        String vmUuid = spec.getVmInstance() == null ? null : spec.getVmInstance().getUuid();
        logger.debug(String.format("allocator domain[uuid:%s, zone:%s] constrains hosts for vm[uuid:%s] scene[%s]",
                domain.domainUuid, domain.zoneUuid, vmUuid, spec.getAllocationScene()));

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(
                domain.rules, candidateHosts(), domain.zoneUuid);
        for (HostCandidate candidate : candidates) {
            String uuid = candidate.getUuid();
            if (!decision.allowedHostUuids.contains(uuid)) {
                reject(candidate, i18m("not in allocator domain[uuid:%s]", domain.domainUuid));
                continue;
            }
            if (decision.recommendedHostUuids.contains(uuid)) {
                recommend(candidate);
            } else if (decision.notRecommendedHostUuids.contains(uuid)) {
                notRecommend(candidate);
            }
        }
    }
}
