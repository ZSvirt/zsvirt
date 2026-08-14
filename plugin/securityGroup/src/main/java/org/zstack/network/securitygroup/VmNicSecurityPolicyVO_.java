package org.zstack.network.securitygroup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmNicSecurityPolicyVO.class)
public class VmNicSecurityPolicyVO_ {
    public static volatile SingularAttribute<VmNicSecurityPolicyVO, String> uuid;
    public static volatile SingularAttribute<VmNicSecurityPolicyVO, String> vmNicUuid;
    public static volatile SingularAttribute<VmNicSecurityPolicyVO, String> ingressPolicy;
    public static volatile SingularAttribute<VmNicSecurityPolicyVO, String> egressPolicy;
    public static volatile SingularAttribute<VmNicSecurityPolicyVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmNicSecurityPolicyVO, Timestamp> lastOpDate;
}
