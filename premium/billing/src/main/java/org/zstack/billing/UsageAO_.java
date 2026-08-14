package org.zstack.billing;

/**
 * Created by xing5 on 2016/9/15.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(UsageAO.class)
public class UsageAO_ {
    public static volatile SingularAttribute<UsageAO, String> accountUuid;
    public static volatile SingularAttribute<UsageAO, Long> dateInLong;
}
