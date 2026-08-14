package org.zstack.billing;

/**
 * Created by xing5 on 2016/9/15.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PriceVO.class)
public class PriceVO_ {
    public static volatile SingularAttribute<PriceVO, String> uuid;
    public static volatile SingularAttribute<PriceVO, String> resourceName;
    public static volatile SingularAttribute<PriceVO, Long> dateInLong;
    public static volatile SingularAttribute<PriceVO, Long> endDateInLong;
    public static volatile SingularAttribute<PriceVO, String> resourceUnit;
    public static volatile SingularAttribute<PriceVO, String> timeUnit;
    public static volatile SingularAttribute<PriceVO, Double> price;
    public static volatile SingularAttribute<PriceVO, Timestamp> createDate;
    public static volatile SingularAttribute<PriceVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<PriceVO, String> tableUuid;
}
