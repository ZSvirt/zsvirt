package org.zstack.allocator.domain.compute;

import org.zstack.allocator.domain.entity.AllocatorHostRule;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainResourceType;
import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.header.host.HostVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hard membership is "what this domain includes":
 * {@code (∪ Include) − (∪ Exclude)} if any Include exists, otherwise {@code all candidates − ∪ Exclude}.
 * Priority does not affect Hard. Soft (Recommend / NotRecommend) is first-match by list order.
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

    public boolean hasHostChain(Collection<AllocatorHostRule> rules) {
        if (rules == null) {
            return false;
        }
        return rules.stream().anyMatch(this::isHostChainRule);
    }

    public HostDecision evaluateHosts(Collection<AllocatorHostRule> rules, List<HostVO> candidates) {
        return evaluateHosts(rules, candidates, null);
    }

    /**
     * {@code domainZoneUuid} is an AND bound (not a union Include): hosts outside that zone are dropped.
     * Soft first-match follows {@code rules} list order.
     */
    public HostDecision evaluateHosts(Collection<AllocatorHostRule> rules, List<HostVO> candidates,
                                      String domainZoneUuid) {
        List<AllocatorHostRule> hostChain = rules == null ? new ArrayList<>() : rules.stream()
                .filter(this::isHostChainRule)
                .collect(Collectors.toList());
        List<AllocatorHostRule> includes = hostChain.stream()
                .filter(rule -> effectOf(rule) == AllocatorDomainRuleEffect.Include)
                .collect(Collectors.toList());
        List<AllocatorHostRule> excludes = hostChain.stream()
                .filter(rule -> effectOf(rule) == AllocatorDomainRuleEffect.Exclude)
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

    public UuidDecision evaluatePrimaryStorages(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.PrimaryStorageVO.name());
    }

    public UuidDecision evaluateL3Networks(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.L3NetworkVO.name());
    }

    /**
     * Hard membership for a uuid resource type, no qualifier split:
     * {@code (∪ Include) − (∪ Exclude)} if any Include exists, otherwise {@code all candidates − ∪ Exclude}.
     * Soft rules of this type do not change membership. {@code hasChain} is false when the domain has no rules of this type.
     */
    private UuidDecision evaluateUuidChain(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids,
                                           String resourceType) {
        List<AllocatorHostRule> chain = rules == null ? new ArrayList<>() : rules.stream()
                .filter(rule -> isUuidChainRule(rule, resourceType))
                .collect(Collectors.toList());
        if (chain.isEmpty()) {
            return new UuidDecision(false, false, new LinkedHashSet<>());
        }

        List<AllocatorHostRule> includes = chain.stream()
                .filter(rule -> effectOf(rule) == AllocatorDomainRuleEffect.Include)
                .collect(Collectors.toList());
        List<AllocatorHostRule> excludes = chain.stream()
                .filter(rule -> effectOf(rule) == AllocatorDomainRuleEffect.Exclude)
                .collect(Collectors.toList());
        boolean hasInclude = !includes.isEmpty();

        Set<String> allowed = new LinkedHashSet<>();
        Collection<String> candidates = candidateUuids == null ? new ArrayList<>() : candidateUuids;
        for (String uuid : candidates) {
            if (uuid == null) {
                continue;
            }
            if (excludes.stream().anyMatch(rule -> uuid.equals(rule.resourceUuid))) {
                continue;
            }
            if (hasInclude && includes.stream().noneMatch(rule -> uuid.equals(rule.resourceUuid))) {
                continue;
            }
            allowed.add(uuid);
        }
        return new UuidDecision(true, hasInclude, allowed);
    }

    private AllocatorDomainRuleEffect firstSoft(List<AllocatorHostRule> hostChain, HostVO host) {
        for (AllocatorHostRule rule : hostChain) {
            AllocatorDomainRuleEffect effect = effectOf(rule);
            if (isSoft(effect) && hits(rule, host)) {
                return effect;
            }
        }
        return null;
    }

    private AllocatorDomainRuleEffect effectOf(AllocatorHostRule rule) {
        if (rule == null || rule.effect == null) {
            return null;
        }
        try {
            return AllocatorDomainRuleEffect.valueOf(rule.effect);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isHostChainRule(AllocatorHostRule rule) {
        if (rule == null || rule.resourceType == null) {
            return false;
        }
        return AllocatorDomainResourceType.HostVO.name().equals(rule.resourceType)
                || AllocatorDomainResourceType.ClusterVO.name().equals(rule.resourceType);
    }

    private boolean isUuidChainRule(AllocatorHostRule rule, String resourceType) {
        if (rule == null || rule.resourceType == null) {
            return false;
        }
        if (resourceType.equals(rule.resourceType)) {
            return true;
        }
        return AllocatorDomainResourceType.L3NetworkVO.name().equals(resourceType)
                && AllocatorDomainConstant.PORT_GROUP_RESOURCE_TYPE.equals(rule.resourceType);
    }

    private boolean hits(AllocatorHostRule rule, HostVO host) {
        if (AllocatorDomainResourceType.HostVO.name().equals(rule.resourceType)) {
            return host.getUuid().equals(rule.resourceUuid);
        }
        if (AllocatorDomainResourceType.ClusterVO.name().equals(rule.resourceType)) {
            return host.getClusterUuid() != null && host.getClusterUuid().equals(rule.resourceUuid);
        }
        return false;
    }

    private boolean isSoft(AllocatorDomainRuleEffect effect) {
        return effect == AllocatorDomainRuleEffect.Recommend || effect == AllocatorDomainRuleEffect.NotRecommend;
    }
}
