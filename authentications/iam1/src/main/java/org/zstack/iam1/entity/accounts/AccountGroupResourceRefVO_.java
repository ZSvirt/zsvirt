package org.zstack.iam1.entity.accounts;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountGroupResourceRefVO.class)
public class AccountGroupResourceRefVO_ {
    public static volatile SingularAttribute<AccountGroupResourceRefVO, Long> id;
    public static volatile SingularAttribute<AccountGroupResourceRefVO, String> groupUuid;
    public static volatile SingularAttribute<AccountGroupResourceRefVO, String> resourceUuid;
    public static volatile SingularAttribute<AccountGroupResourceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountGroupResourceRefVO, Timestamp> lastOpDate;
}
