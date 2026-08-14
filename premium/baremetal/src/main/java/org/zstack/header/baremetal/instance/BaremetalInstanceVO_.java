package org.zstack.header.baremetal.instance;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 7/4/18.
 */
@StaticMetamodel(BaremetalInstanceVO.class)
public class BaremetalInstanceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<BaremetalInstanceVO, String> name;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> description;
    public static volatile SingularAttribute<BaremetalInstanceVO, Long> internalId;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> zoneUuid;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> clusterUuid;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> pxeServerUuid;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> chassisUuid;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> imageUuid;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> templateUuid;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> platform;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> managementIp;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> username;
    public static volatile SingularAttribute<BaremetalInstanceVO, String> password;
    public static volatile SingularAttribute<BaremetalInstanceVO, Integer> port;
    public static volatile SingularAttribute<BaremetalInstanceVO, BaremetalInstanceState> state;
    public static volatile SingularAttribute<BaremetalInstanceVO, BaremetalInstanceStatus> status;
    public static volatile SingularAttribute<BaremetalInstanceVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalInstanceVO, Timestamp> lastOpDate;
}
