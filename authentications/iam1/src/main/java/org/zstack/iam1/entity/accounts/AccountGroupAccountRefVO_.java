package org.zstack.iam1.entity.accounts;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountGroupAccountRefVO.class)
public class AccountGroupAccountRefVO_ {
    public static volatile SingularAttribute<AccountGroupAccountRefVO, Long> id;
    public static volatile SingularAttribute<AccountGroupAccountRefVO, String> groupUuid;
    public static volatile SingularAttribute<AccountGroupAccountRefVO, String> accountUuid;
    public static volatile SingularAttribute<AccountGroupAccountRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountGroupAccountRefVO, Timestamp> lastOpDate;
}
