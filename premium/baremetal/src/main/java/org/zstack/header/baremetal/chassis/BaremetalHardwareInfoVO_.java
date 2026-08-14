package org.zstack.header.baremetal.chassis;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 6/23/17.
 */

@StaticMetamodel(BaremetalHardwareInfoVO.class)
public class BaremetalHardwareInfoVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<BaremetalHardwareInfoVO, String> chassisUuid;
    public static volatile SingularAttribute<BaremetalHardwareInfoVO, String> type;
    public static volatile SingularAttribute<BaremetalHardwareInfoVO, String> content;
    public static volatile SingularAttribute<BaremetalHardwareInfoVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalHardwareInfoVO, Timestamp> lastOpDate;
}
