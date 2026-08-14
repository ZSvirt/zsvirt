package org.zstack.billing.table;

/**
 * Created by lining on 2019/9/10.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountPriceTableRefVO.class)
public class AccountPriceTableRefVO_ {
    public static volatile SingularAttribute<AccountPriceTableRefVO, String> accountUuid;
    public static volatile SingularAttribute<AccountPriceTableRefVO, String> tableUuid;
    public static volatile SingularAttribute<AccountPriceTableRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountPriceTableRefVO, Timestamp> lastOpDate;
}
