package org.zstack.network.service.portforwarding;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PortForwardingRuleVO.class)
public class PortForwardingRuleVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PortForwardingRuleVO, String> name;
    public static volatile SingularAttribute<PortForwardingRuleVO, String> description;
    public static volatile SingularAttribute<PortForwardingRuleVO, String> vipUuid;
    public static volatile SingularAttribute<PortForwardingRuleVO, Integer> vipPortStart;
    public static volatile SingularAttribute<PortForwardingRuleVO, Integer> vipPortEnd;
    public static volatile SingularAttribute<PortForwardingRuleVO, Integer> privatePortStart;
    public static volatile SingularAttribute<PortForwardingRuleVO, Integer> privatePortEnd;
    public static volatile SingularAttribute<PortForwardingRuleVO, String> vmNicUuid;
    public static volatile SingularAttribute<PortForwardingRuleVO, String> vipIp;
    public static volatile SingularAttribute<PortForwardingRuleVO, String> guestIp;
    public static volatile SingularAttribute<PortForwardingRuleVO, String> allowedCidr;
    public static volatile SingularAttribute<PortForwardingRuleVO, PortForwardingRuleState> state;
    public static volatile SingularAttribute<PortForwardingRuleVO, PortForwardingProtocolType> protocolType;
    public static volatile SingularAttribute<PortForwardingRuleVO, Timestamp> createDate;
    public static volatile SingularAttribute<PortForwardingRuleVO, Timestamp> lastOpDate;
}
