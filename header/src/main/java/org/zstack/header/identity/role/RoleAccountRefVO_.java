package org.zstack.header.identity.role;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RoleAccountRefVO.class)
public class RoleAccountRefVO_ {
    public static volatile SingularAttribute<RoleAccountRefVO, String> roleUuid;
    public static volatile SingularAttribute<RoleAccountRefVO, String> accountUuid;
    public static volatile SingularAttribute<RoleAccountRefVO, String> accountPermissionFrom;
    public static volatile SingularAttribute<RoleAccountRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<RoleAccountRefVO, Timestamp> lastOpDate;
}
