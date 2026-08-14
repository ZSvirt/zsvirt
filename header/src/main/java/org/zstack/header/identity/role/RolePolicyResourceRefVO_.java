package org.zstack.header.identity.role;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(RolePolicyResourceRefVO.class)
public class RolePolicyResourceRefVO_ {
    public static volatile SingularAttribute<RolePolicyResourceRefVO, Long> id;
    public static volatile SingularAttribute<RolePolicyResourceRefVO, Long> rolePolicyId;
    public static volatile SingularAttribute<RolePolicyResourceRefVO, RolePolicyResourceEffect> effect;
    public static volatile SingularAttribute<RolePolicyResourceRefVO, String> resourceUuid;
}
