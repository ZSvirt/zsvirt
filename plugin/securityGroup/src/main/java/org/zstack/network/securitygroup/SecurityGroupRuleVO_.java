package org.zstack.network.securitygroup;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SecurityGroupRuleVO.class)
public class SecurityGroupRuleVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> securityGroupUuid;
    public static volatile SingularAttribute<SecurityGroupRuleVO, SecurityGroupRuleType> type;
    public static volatile SingularAttribute<SecurityGroupRuleVO, SecurityGroupRuleState> state;
    public static volatile SingularAttribute<SecurityGroupRuleVO, Integer> ipVersion;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> protocol;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> remoteSecurityGroupUuid;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> description;
    public static volatile SingularAttribute<SecurityGroupRuleVO, Integer> priority;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> action;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> srcIpRange;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> dstIpRange;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> srcPortRange;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> dstPortRange;
    public static volatile SingularAttribute<SecurityGroupRuleVO, Integer> startPort;
    public static volatile SingularAttribute<SecurityGroupRuleVO, Integer> endPort;
    public static volatile SingularAttribute<SecurityGroupRuleVO, String> allowedCidr;
    public static volatile SingularAttribute<SecurityGroupRuleVO, Timestamp> createDate;
    public static volatile SingularAttribute<SecurityGroupRuleVO, Timestamp> lastOpDate;
}
