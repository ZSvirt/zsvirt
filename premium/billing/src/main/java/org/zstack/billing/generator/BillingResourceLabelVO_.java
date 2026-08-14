package org.zstack.billing.generator;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(BillingResourceLabelVO.class)
public class BillingResourceLabelVO_ {
    public static volatile SingularAttribute<BillingResourceLabelVO, String> resourceUuid;
    public static volatile SingularAttribute<BillingResourceLabelVO, String> labelKey;
    public static volatile SingularAttribute<BillingResourceLabelVO, String> labelValue;

}
