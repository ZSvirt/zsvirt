package org.zstack.allocator.domain.entity;

import org.zstack.header.allocator.AllocatorDomainScene;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AllocatorDomainSceneVO.class)
public class AllocatorDomainSceneVO_ {
    public static volatile SingularAttribute<AllocatorDomainSceneVO, Long> id;
    public static volatile SingularAttribute<AllocatorDomainSceneVO, String> domainUuid;
    public static volatile SingularAttribute<AllocatorDomainSceneVO, AllocatorDomainScene> scene;
    public static volatile SingularAttribute<AllocatorDomainSceneVO, Timestamp> createDate;
}
