package org.zstack.iam1.entity.accounts;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountGroupRoleRefVO.class)
public class AccountGroupRoleRefVO_ {
    public static volatile SingularAttribute<AccountGroupRoleRefVO, Long> id;
    public static volatile SingularAttribute<AccountGroupRoleRefVO, String> groupUuid;
    public static volatile SingularAttribute<AccountGroupRoleRefVO, String> roleUuid;
    public static volatile SingularAttribute<AccountGroupRoleRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountGroupRoleRefVO, Timestamp> lastOpDate;
}
