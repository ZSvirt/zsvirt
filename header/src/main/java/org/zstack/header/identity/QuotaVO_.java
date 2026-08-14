package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.security.Timestamp;

/**
 * Created by frank on 7/13/2015.
 */
@StaticMetamodel(QuotaVO.class)
public class QuotaVO_ {
    public static volatile SingularAttribute<QuotaVO, Long> id;
    public static volatile SingularAttribute<QuotaVO, String> name;
    public static volatile SingularAttribute<QuotaVO, String> identityUuid;
    public static volatile SingularAttribute<QuotaVO, String> identityType;
    public static volatile SingularAttribute<QuotaVO, Long> value;
    public static volatile SingularAttribute<QuotaVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<QuotaVO, Timestamp> createDate;
}
