package org.zstack.sns.platform.snmp;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.sns.SNSApplicationPlatformVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SNSSnmpPlatformVO.class)
public class SNSSnmpPlatformVO_ extends SNSApplicationPlatformVO_ {
    public static volatile SingularAttribute<SNSSnmpPlatformVO, String> snmpAddress;
    public static volatile SingularAttribute<SNSSnmpPlatformVO, Integer> snmpPort;
}
