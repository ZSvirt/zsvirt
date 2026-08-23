package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AllocatorDomainRuleVO.class)
public class AllocatorDomainRuleVO_ {
    public static volatile SingularAttribute<AllocatorDomainRuleVO, Long> id;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> domainUuid;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, Integer> priority;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> resourceUuid;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> resourceType;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> resourceQualifier;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> resourcePolicy;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, AllocatorDomainRuleEffect> effect;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> policyFrom;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, String> policyUuid;
    public static volatile SingularAttribute<AllocatorDomainRuleVO, Timestamp> createDate;
}
