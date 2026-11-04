package org.zstack.allocator.domain.compute;

import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.allocator.domain.AllocatorHostRule;
import org.zstack.header.allocator.domain.AllocatorHostRuleSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AllocatorDomains {
    private AllocatorDomains() {
    }

    public static AllocatorHostRuleSet transform(AllocatorDomainVO vo) {
        if (vo == null) {
            return null;
        }
        AllocatorHostRuleSet set = AllocatorHostRuleSet.domain(vo.getUuid());
        set.zoneUuid = vo.getZoneUuid();
        List<AllocatorDomainRuleVO> ordered = new ArrayList<>();
        if (vo.getRules() != null) {
            ordered.addAll(vo.getRules());
            ordered.sort(Comparator.comparingInt(AllocatorDomainRuleVO::getPriority));
        }
        for (AllocatorDomainRuleVO rule : ordered) {
            AllocatorHostRule hostRule = transform(rule);
            if (hostRule != null) {
                set.rules.add(hostRule);
            }
        }
        return set;
    }

    public static AllocatorHostRule transform(AllocatorDomainRuleVO vo) {
        if (vo == null) {
            return null;
        }
        AllocatorHostRule rule = new AllocatorHostRule();
        rule.effect = vo.getEffect() == null ? null : vo.getEffect().toString();
        rule.resourceType = vo.getResourceType();
        rule.resourceUuid = vo.getResourceUuid();
        rule.resourceQualifier = vo.getResourceQualifier();
        return rule;
    }
}
