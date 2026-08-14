package org.zstack.snmp.agent;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.snmp.agent.SecurityLevel;
import org.zstack.snmp.agent.SnmpAgentAuthAlgorithm;
import org.zstack.snmp.agent.SnmpAgentPrivacyAlgorithm;
import org.zstack.snmp.agent.SnmpAgentVersion;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SnmpAgentVO.class)
public class SnmpAgentVO_ {
    public static volatile SingularAttribute<SnmpAgentVO, String> uuid;
    public static volatile SingularAttribute<SnmpAgentVO, SnmpAgentVersion> version;
    public static volatile SingularAttribute<SnmpAgentVO, String> readCommunity;
    public static volatile SingularAttribute<SnmpAgentVO, String> userName;
    public static volatile SingularAttribute<SnmpAgentVO, SnmpAgentAuthAlgorithm> authAlgorithm;
    public static volatile SingularAttribute<SnmpAgentVO, String> authPassword;
    public static volatile SingularAttribute<SnmpAgentVO, SnmpAgentPrivacyAlgorithm> privacyAlgorithm;
    public static volatile SingularAttribute<SnmpAgentVO, String> privacyPassword;
    public static volatile SingularAttribute<SnmpAgentVO, Integer> port;
    public static volatile SingularAttribute<SnmpAgentVO, SnmpAgentStatus> status;
    public static volatile SingularAttribute<SnmpAgentVO, SecurityLevel> securityLevel;
    public static volatile SingularAttribute<SnmpAgentVO, Timestamp> createDate;
    public static volatile SingularAttribute<SnmpAgentVO, Timestamp> lastOpDate;
}
