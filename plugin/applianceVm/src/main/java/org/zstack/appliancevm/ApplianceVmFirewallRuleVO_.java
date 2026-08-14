package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstanceAO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(ApplianceVmFirewallRuleVO.class)
public class ApplianceVmFirewallRuleVO_ {
    public static volatile SingularAttribute<ApplianceVmVO, Long> id;
    public static volatile SingularAttribute<ApplianceVmVO, String> applianceVmUuid;
    public static volatile SingularAttribute<ApplianceVmVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<ApplianceVmVO, String> sourceIp;
    public static volatile SingularAttribute<ApplianceVmVO, String> destIp;
    public static volatile SingularAttribute<ApplianceVmVO, ApplianceVmFirewallProtocol> protocol;
    public static volatile SingularAttribute<ApplianceVmVO, Integer> startPort;
    public static volatile SingularAttribute<ApplianceVmVO, Integer> endPort;
    public static volatile SingularAttribute<ApplianceVmVO, String> allowCidr;
    public static volatile SingularAttribute<ApplianceVmVO, String> identity;
    public static volatile SingularAttribute<VmInstanceAO, Timestamp> createDate;
    public static volatile SingularAttribute<VmInstanceAO, Timestamp> lastOpDate;
}
