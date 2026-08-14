package org.zstack.billing.generator;

/**
 * Created by lining on 2019/4/2.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(BillingVO.class)
public class BillingVO_ {
    public static volatile SingularAttribute<BillingVO, Long> id;
    public static volatile SingularAttribute<BillingVO, BillingType> billingType;
    public static volatile SingularAttribute<BillingVO, String> accountUuid;
    public static volatile SingularAttribute<BillingVO, String> resourceUuid;
    public static volatile SingularAttribute<BillingVO, String> resourceName;
    public static volatile SingularAttribute<BillingVO, Double> spending;
    public static volatile SingularAttribute<BillingVO, Long> startTime;
    public static volatile SingularAttribute<BillingVO, Long> endTime;
    public static volatile SingularAttribute<BillingVO, Timestamp> createDate;
    public static volatile SingularAttribute<BillingVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<BillingVO, String> hypervisorType;
}
