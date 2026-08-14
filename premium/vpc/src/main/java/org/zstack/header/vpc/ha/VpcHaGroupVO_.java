package org.zstack.header.vpc.ha;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VpcHaGroupVO.class)
public class VpcHaGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VpcHaGroupVO, String> name;
    public static volatile SingularAttribute<VpcHaGroupVO, String> description;
    public static volatile SingularAttribute<VpcHaGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<VpcHaGroupVO, Timestamp> lastOpDate;
}
