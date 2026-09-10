package org.zstack.allocator.domain.compute;

import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainResourceType;
import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.header.host.HostVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hard membership is "what this domain includes":
 * {@code (∪ Include) − (∪ Exclude)} if any Include exists, otherwise {@code all candidates − ∪ Exclude}.
 * Priority does not affect Hard. Soft (Recommend / NotRecommend) is still first-match by priority.
 * {@code domainZoneUuid} is an AND bound, not a union Include.
 */
public class AllocatorDomainRuleEvaluator {
    public static class HostDecision {
        public Set<String> allowedHostUuids;
        public Set<String> recommendedHostUuids;
        public Set<String> notRecommendedHostUuids;
        public boolean hasInclude;

        public HostDecision(Set<String> allowedHostUuids, Set<String> recommendedHostUuids,
                            Set<String> notRecommendedHostUuids, boolean hasInclude) {
            this.allowedHostUuids = allowedHostUuids;
            this.recommendedHostUuids = recommendedHostUuids;
            this.notRecommendedHostUuids = notRecommendedHostUuids;
            this.hasInclude = hasInclude;
        }
    }

    public HostDecision evaluateHosts(Collection<AllocatorDomainRuleVO> rules, List<HostVO> candidates) {
        return evaluateHosts(rules, candidates, null);
    }

    /**
     * {@code domainZoneUuid} is an AND bound (not a union Include): hosts outside that zone are dropped.
     */
    public HostDecision evaluateHosts(Collection<AllocatorDomainRuleVO> rules, List<HostVO> candidates,
                                      String domainZoneUuid) {
        List<AllocatorDomainRuleVO> hostChain = rules == null ? new ArrayList<>() : rules.stream()
                .filter(this::isHostChainRule)
                .sorted(Comparator.comparingInt(AllocatorDomainRuleVO::getPriority))
                .collect(Collectors.toList());
        List<AllocatorDomainRuleVO> includes = hostChain.stream()
                .filter(rule -> rule.getEffect() == AllocatorDomainRuleEffect.Include)
                .collect(Collectors.toList());
        List<AllocatorDomainRuleVO> excludes = hostChain.stream()
                .filter(rule -> rule.getEffect() == AllocatorDomainRuleEffect.Exclude)
                .collect(Collectors.toList());
        boolean hasInclude = !includes.isEmpty();

        Set<String> allowed = new LinkedHashSet<>();
        Set<String> recommended = new LinkedHashSet<>();
        Set<String> notRecommended = new LinkedHashSet<>();
        List<HostVO> hosts = candidates == null ? new ArrayList<>() : candidates;
        for (HostVO host : hosts) {
            if (domainZoneUuid != null && !domainZoneUuid.equals(host.getZoneUuid())) {
                continue;
            }
            if (excludes.stream().anyMatch(rule -> hits(rule, host))) {
                continue;
            }
            if (hasInclude && includes.stream().noneMatch(rule -> hits(rule, host))) {
                continue;
            }

            allowed.add(host.getUuid());
            AllocatorDomainRuleEffect soft = firstSoft(hostChain, host);
            if (soft == AllocatorDomainRuleEffect.Recommend) {
                recommended.add(host.getUuid());
            } else if (soft == AllocatorDomainRuleEffect.NotRecommend) {
                notRecommended.add(host.getUuid());
            }
        }
        return new HostDecision(allowed, recommended, notRecommended, hasInclude);
    }

    public static class UuidDecision {
        public boolean hasChain;
        public boolean hasInclude;
        public Set<String> allowedUuids;

        public UuidDecision(boolean hasChain, boolean hasInclude, Set<String> allowedUuids) {
            this.hasChain = hasChain;
            this.hasInclude = hasInclude;
            this.allowedUuids = allowedUuids;
        }
    }

    public UuidDecision evaluatePrimaryStorages(Collection<AllocatorDomainRuleVO> rules, Collection<String> candidateUuids) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.PrimaryStorageVO.name());
    }

    public UuidDecision evaluateL3Networks(Collection<AllocatorDomainRuleVO> rules, Collection<String> candidateUuids) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.L3NetworkVO.name());
    }

    /**
     * Hard membership for a uuid resource type, no qualifier split:
     * {@code (∪ Include) − (∪ Exclude)} if any Include exists, otherwise {@code all candidates − ∪ Exclude}.
     * Soft rules of this type do not change membership. {@code hasChain} is false when the domain has no rules of this type.
     */
    private UuidDecision evaluateUuidChain(Collection<AllocatorDomainRuleVO> rules, Collection<String> candidateUuids,
                                           String resourceType) {
        List<AllocatorDomainRuleVO> chain = rules == null ? new ArrayList<>() : rules.stream()
                .filter(rule -> isUuidChainRule(rule, resourceType))
                .collect(Collectors.toList());
        if (chain.isEmpty()) {
            return new UuidDecision(false, false, new LinkedHashSet<>());
        }

        List<AllocatorDomainRuleVO> includes = chain.stream()
                .filter(rule -> rule.getEffect() == AllocatorDomainRuleEffect.Include)
                .collect(Collectors.toList());
        List<AllocatorDomainRuleVO> excludes = chain.stream()
                .filter(rule -> rule.getEffect() == AllocatorDomainRuleEffect.Exclude)
                .collect(Collectors.toList());
        boolean hasInclude = !includes.isEmpty();

        Set<String> allowed = new LinkedHashSet<>();
        Collection<String> candidates = candidateUuids == null ? new ArrayList<>() : candidateUuids;
        for (String uuid : candidates) {
            if (uuid == null) {
                continue;
            }
            if (excludes.stream().anyMatch(rule -> uuid.equals(rule.getResourceUuid()))) {
                continue;
            }
            if (hasInclude && includes.stream().noneMatch(rule -> uuid.equals(rule.getResourceUuid()))) {
                continue;
            }
            allowed.add(uuid);
        }
        return new UuidDecision(true, hasInclude, allowed);
    }

    private AllocatorDomainRuleEffect firstSoft(List<AllocatorDomainRuleVO> hostChain, HostVO host) {
        for (AllocatorDomainRuleVO rule : hostChain) {
            if (isSoft(rule.getEffect()) && hits(rule, host)) {
                return rule.getEffect();
            }
        }
        return null;
    }

    private boolean isHostChainRule(AllocatorDomainRuleVO rule) {
        if (rule == null || rule.getResourceType() == null) {
            return false;
        }
        return AllocatorDomainResourceType.HostVO.name().equals(rule.getResourceType())
                || AllocatorDomainResourceType.ClusterVO.name().equals(rule.getResourceType());
    }

    private boolean isUuidChainRule(AllocatorDomainRuleVO rule, String resourceType) {
        if (rule == null || rule.getResourceType() == null) {
            return false;
        }
        if (resourceType.equals(rule.getResourceType())) {
            return true;
        }
        return AllocatorDomainResourceType.L3NetworkVO.name().equals(resourceType)
                && AllocatorDomainConstant.PORT_GROUP_RESOURCE_TYPE.equals(rule.getResourceType());
    }

    private boolean hits(AllocatorDomainRuleVO rule, HostVO host) {
        if (AllocatorDomainResourceType.HostVO.name().equals(rule.getResourceType())) {
            return host.getUuid().equals(rule.getResourceUuid());
        }
        if (AllocatorDomainResourceType.ClusterVO.name().equals(rule.getResourceType())) {
            return host.getClusterUuid() != null && host.getClusterUuid().equals(rule.getResourceUuid());
        }
        return false;
    }

    private boolean isSoft(AllocatorDomainRuleEffect effect) {
        return effect == AllocatorDomainRuleEffect.Recommend || effect == AllocatorDomainRuleEffect.NotRecommend;
    }
}
