package org.zstack.network.hostNetworkInterface;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 4/24/20.
 */
@StaticMetamodel(HostNetworkBondingVO.class)
public class HostNetworkBondingVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostNetworkBondingVO, String> hostUuid;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> bondingName;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> bondingType;
    public static volatile SingularAttribute<HostNetworkBondingVO, Long> speed;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> mode;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> xmitHashPolicy;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> miiStatus;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> mac;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> ipAddresses;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> gateway;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> callBackIp;
    public static volatile SingularAttribute<HostNetworkBondingVO, Long> miimon;
    public static volatile SingularAttribute<HostNetworkBondingVO, Boolean> allSlavesActive;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> type;
    public static volatile SingularAttribute<HostNetworkBondingVO, String> description;
    public static volatile SingularAttribute<HostNetworkBondingVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkBondingVO, Timestamp> lastOpDate;
}
