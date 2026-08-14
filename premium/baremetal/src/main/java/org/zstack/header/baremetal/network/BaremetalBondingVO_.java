package org.zstack.header.baremetal.network;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-01-03.
 */
@StaticMetamodel(BaremetalBondingVO.class)
public class BaremetalBondingVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<BaremetalBondingVO, String> chassisUuid;
    public static volatile SingularAttribute<BaremetalBondingVO, String> name;
    public static volatile SingularAttribute<BaremetalBondingVO, Integer> mode;
    public static volatile SingularAttribute<BaremetalBondingVO, String> slaves;
    public static volatile SingularAttribute<BaremetalBondingVO, String> opts;
    public static volatile SingularAttribute<BaremetalBondingVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalBondingVO, Timestamp> lastOpDate;
}
