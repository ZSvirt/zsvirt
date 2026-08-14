package org.zstack.header.identity.role;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RolePolicyVO.class)
public class RolePolicyVO_ {
    public static volatile SingularAttribute<RolePolicyVO, Long> id;
    public static volatile SingularAttribute<RolePolicyVO, String> roleUuid;
    public static volatile SingularAttribute<RolePolicyVO, String> actions;
    public static volatile SingularAttribute<RolePolicyVO, RolePolicyEffect> effect;
    public static volatile SingularAttribute<RolePolicyVO, String> resourceType;
    public static volatile SingularAttribute<RolePolicyVO, Timestamp> createDate;
}
