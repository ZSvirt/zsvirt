package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AllocatorDomainVO.class)
public class AllocatorDomainVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AllocatorDomainVO, String> name;
    public static volatile SingularAttribute<AllocatorDomainVO, String> description;
    public static volatile SingularAttribute<AllocatorDomainVO, String> zoneUuid;
    public static volatile SingularAttribute<AllocatorDomainVO, AllocatorDomainState> state;
    public static volatile SingularAttribute<AllocatorDomainVO, Timestamp> createDate;
    public static volatile SingularAttribute<AllocatorDomainVO, Timestamp> lastOpDate;
    public static volatile ListAttribute<AllocatorDomainVO, AllocatorDomainRuleVO> rules;
    public static volatile SetAttribute<AllocatorDomainVO, AllocatorDomainSceneVO> scenes;
}
