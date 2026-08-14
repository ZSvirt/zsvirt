package org.zstack.header.baremetal.network;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 7/5/18.
 */
@StaticMetamodel(BaremetalNicVO.class)
public class BaremetalNicVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<BaremetalNicVO, String> baremetalInstanceUuid;
    public static volatile SingularAttribute<BaremetalNicVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<BaremetalNicVO, String> usedIpUuid;
    public static volatile SingularAttribute<BaremetalNicVO, String> baremetalBondingUuid;
    public static volatile SingularAttribute<BaremetalNicVO, String> mac;
    public static volatile SingularAttribute<BaremetalNicVO, String> ip;
    public static volatile SingularAttribute<BaremetalNicVO, String> netmask;
    public static volatile SingularAttribute<BaremetalNicVO, String> gateway;
    public static volatile SingularAttribute<BaremetalNicVO, String> metadata;
    public static volatile SingularAttribute<BaremetalNicVO, Boolean> pxe;
    public static volatile SingularAttribute<BaremetalNicVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalNicVO, Timestamp> lastOpDate;
}
