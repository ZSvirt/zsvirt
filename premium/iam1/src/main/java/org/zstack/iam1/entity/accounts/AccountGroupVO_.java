package org.zstack.iam1.entity.accounts;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountGroupVO.class)
public class AccountGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AccountGroupVO, String> name;
    public static volatile SingularAttribute<AccountGroupVO, String> description;
    public static volatile SingularAttribute<AccountGroupVO, String> parentUuid;
    public static volatile SingularAttribute<AccountGroupVO, String> rootGroupUuid;
    public static volatile SingularAttribute<AccountGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountGroupVO, Timestamp> lastOpDate;
}
