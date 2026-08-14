package org.zstack.header.affinitygroup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by shixin on 10/24/2017.
 */
@StaticMetamodel(AffinityGroupUsageVO.class)
public class AffinityGroupUsageVO_ {
    public static volatile SingularAttribute<AffinityGroupUsageVO, String> uuid;
    public static volatile SingularAttribute<AffinityGroupUsageVO, String> affinityGroupUuid;
    public static volatile SingularAttribute<AffinityGroupUsageVO, String> resourceUuid;
    public static volatile SingularAttribute<AffinityGroupUsageVO, String> resourceType;
    public static volatile SingularAttribute<AffinityGroupUsageVO, Timestamp> createDate;
    public static volatile SingularAttribute<AffinityGroupUsageVO, Timestamp> lastOpDate;
}
