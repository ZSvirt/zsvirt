package org.zstack.header.baremetal.network;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(BaremetalVlanNicVO.class)
public class BaremetalVlanNicVO_ extends BaremetalNicVO_ {
    public static volatile SingularAttribute<BaremetalVlanNicVO, Integer> vlan;
}
