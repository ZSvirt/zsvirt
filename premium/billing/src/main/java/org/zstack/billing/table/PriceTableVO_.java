package org.zstack.billing.table;

/**
 * Created by lining on 2019/9/10.
 */

import org.zstack.billing.PriceVO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PriceTableVO.class)
public class PriceTableVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PriceTableVO, String> name;
    public static volatile SingularAttribute<PriceTableVO, String> description;
    public static volatile SingularAttribute<PriceTableVO, Timestamp> createDate;
    public static volatile SingularAttribute<PriceTableVO, Timestamp> lastOpDate;
}
