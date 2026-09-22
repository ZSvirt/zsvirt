package org.zstack.allocator.domain.compute.evaluator;

import org.zstack.header.allocator.domain.AllocatorHostRule;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainResourceType;
import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.filter;
import static org.zstack.utils.CollectionUtils.transform;

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

    private static class HostCandidate {
        public final String hostUuid;
        public final String clusterUuid;
        public final String zoneUuid;
        /**
         * include / recommended / notRecommended -> latter overrides rule
         */
        public boolean include, recommended, notRecommended;

        public HostCandidate(HostVO host) {
            this.hostUuid = Objects.requireNonNull(host.getUuid());
            this.clusterUuid = Objects.requireNonNull(host.getClusterUuid());
            this.zoneUuid = Objects.requireNonNull(host.getZoneUuid());
        }

        private void apply(AllocatorDomainRuleEffect effect) {
            switch (effect) {
            case Include: {
                include = true;
            } break;
            case Exclude: {
                include = false;
                recommended = false;
                notRecommended = false;
            } break;
            case Recommend: {
                if (include) {
                    recommended = true;
                    notRecommended = false;
                }
            } break;
            case NotRecommend: {
                if (include) {
                    recommended = false;
                    notRecommended = true;
                }
            } break;
            }
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

    public HostDecision evaluateHosts(Collection<AllocatorHostRule> rules, List<HostVO> list,
                                      String domainZoneUuid) {
        List<AllocatorHostRule> hostChain = rules == null ? new ArrayList<>() : rules.stream()
                .filter(this::isHostChainRule)
                .collect(Collectors.toList());

        List<HostCandidate> candidates = transform(list, HostCandidate::new);
        boolean hasInclude = hostChain.stream()
                .anyMatch(rule -> effectOf(rule) == AllocatorDomainRuleEffect.Include);
        if (!hasInclude) {
            // if rule set have not included rule, it is a black list
            if (domainZoneUuid == null) {
                candidates.forEach(c -> c.include = true);
            } else {
                candidates.stream()
                        .filter(c -> c.zoneUuid.equals(domainZoneUuid))
                        .forEach(c -> c.include = true);
            }
        }

        for (AllocatorHostRule rule : hostChain) {
            AllocatorDomainRuleEffect effect = effectOf(rule);
            if (effect == null) {
                continue;
            }

            if (HostVO.class.getSimpleName().equals(rule.resourceType)) {
                filter(candidates, c -> c.hostUuid.equals(rule.resourceUuid))
                        .forEach(c -> c.apply(effect));
            } else if (ClusterVO.class.getSimpleName().equals(rule.resourceType)) {
                filter(candidates, c -> c.clusterUuid.equals(rule.resourceUuid))
                        .forEach(c -> c.apply(effect));
            }
        }

        Set<String> allowed = new LinkedHashSet<>();
        Set<String> recommended = new LinkedHashSet<>();
        Set<String> notRecommended = new LinkedHashSet<>();
        for (HostCandidate candidate : candidates) {
            if (!candidate.include) {
                continue;
            }

            allowed.add(candidate.hostUuid);
            if (candidate.recommended) {
                recommended.add(candidate.hostUuid);
            } else if (candidate.notRecommended) {
                notRecommended.add(candidate.hostUuid);
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

    private static class UuidCandidate {
        public final String uuid;
        /**
         * include / recommended / notRecommended -> latter overrides rule
         */
        public boolean include, recommended, notRecommended;

        public UuidCandidate(String uuid) {
            this.uuid = uuid;
        }

        private void apply(AllocatorDomainRuleEffect effect) {
            switch (effect) {
            case Include: {
                include = true;
            } break;
            case Exclude: {
                include = false;
                recommended = false;
                notRecommended = false;
            } break;
            case Recommend: {
                if (include) {
                    recommended = true;
                    notRecommended = false;
                }
            } break;
            case NotRecommend: {
                if (include) {
                    recommended = false;
                    notRecommended = true;
                }
            } break;
            }
        }
    }

    public UuidDecision evaluatePrimaryStorages(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.PrimaryStorageVO.name(), null);
    }

    /**
     * {@code qualifier} Root / Data keeps empty-qualifier rules plus that qualifier.
     * Null qualifier keeps every PrimaryStorageVO rule (host chain / interceptor).
     */
    public UuidDecision evaluatePrimaryStorages(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids,
                                                String qualifier) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.PrimaryStorageVO.name(), qualifier);
    }

    public UuidDecision evaluateL3Networks(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids) {
        return evaluateUuidChain(rules, candidateUuids, AllocatorDomainResourceType.L3NetworkVO.name(), null);
    }

    /**
     * Sequential latter-override for a uuid resource type, same as the host/cluster chain:
     * every matching rule is applied in list order and a later Include/Exclude replaces
     * the earlier verdict for that uuid. If any Include exists, candidates start excluded
     * (whitelist); otherwise they start included (blacklist). Soft rules of this type do
     * not change membership. {@code hasChain} is false when no rules of this type remain.
     */
    private UuidDecision evaluateUuidChain(Collection<AllocatorHostRule> rules, Collection<String> candidateUuids,
                                           String resourceType, String qualifier) {
        List<AllocatorHostRule> chain = rules == null ? new ArrayList<>() : rules.stream()
                .filter(rule -> isUuidChainRule(rule, resourceType))
                .filter(rule -> appliesToQualifier(rule, qualifier))
                .collect(Collectors.toList());
        if (chain.isEmpty()) {
            return new UuidDecision(false, false, new LinkedHashSet<>());
        }

        boolean hasInclude = chain.stream()
                .anyMatch(rule -> effectOf(rule) == AllocatorDomainRuleEffect.Include);

        List<UuidCandidate> candidates = new ArrayList<>();
        Collection<String> uuids = candidateUuids == null ? new ArrayList<>() : candidateUuids;
        for (String uuid : uuids) {
            if (uuid == null) {
                continue;
            }
            candidates.add(new UuidCandidate(uuid));
        }
        if (!hasInclude) {
            candidates.forEach(c -> c.include = true);
        }

        for (AllocatorHostRule rule : chain) {
            AllocatorDomainRuleEffect effect = effectOf(rule);
            if (effect == null) {
                continue;
            }
            filter(candidates, c -> c.uuid.equals(rule.resourceUuid))
                    .forEach(c -> c.apply(effect));
        }

        Set<String> allowed = new LinkedHashSet<>();
        for (UuidCandidate candidate : candidates) {
            if (candidate.include) {
                allowed.add(candidate.uuid);
            }
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

    private boolean appliesToQualifier(AllocatorHostRule rule, String qualifier) {
        if (qualifier == null) {
            return true;
        }
        String ruleQualifier = rule.resourceQualifier;
        return ruleQualifier == null || ruleQualifier.isEmpty() || qualifier.equals(ruleQualifier);
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
