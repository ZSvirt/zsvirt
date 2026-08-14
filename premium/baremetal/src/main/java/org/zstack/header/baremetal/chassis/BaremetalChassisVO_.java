package org.zstack.header.baremetal.chassis;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 4/26/17.
 */
@StaticMetamodel(BaremetalChassisVO.class)
public class BaremetalChassisVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<BaremetalChassisVO, String> name;
    public static volatile SingularAttribute<BaremetalChassisVO, String> description;
    public static volatile SingularAttribute<BaremetalChassisVO, String> zoneUuid;
    public static volatile SingularAttribute<BaremetalChassisVO, String> clusterUuid;
    public static volatile SingularAttribute<BaremetalChassisVO, String> pxeServerUuid;
    public static volatile SingularAttribute<BaremetalChassisVO, String> ipmiAddress;
    public static volatile SingularAttribute<BaremetalChassisVO, Integer> ipmiPort;
    public static volatile SingularAttribute<BaremetalChassisVO, String> ipmiUsername;
    public static volatile SingularAttribute<BaremetalChassisVO, String> ipmiPassword;
    public static volatile SingularAttribute<BaremetalChassisVO, BaremetalChassisState> state;
    public static volatile SingularAttribute<BaremetalChassisVO, BaremetalChassisStatus> status;
    public static volatile SingularAttribute<BaremetalChassisVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalChassisVO, Timestamp> lastOpDate;
}

