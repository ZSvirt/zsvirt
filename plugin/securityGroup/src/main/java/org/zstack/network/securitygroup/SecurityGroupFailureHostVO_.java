package org.zstack.network.securitygroup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SecurityGroupFailureHostVO.class)
public class SecurityGroupFailureHostVO_ {
    public static volatile SingularAttribute<SecurityGroupFailureHostVO_, Integer> id;
    public static volatile SingularAttribute<SecurityGroupFailureHostVO_, String> hostUuid;
    public static volatile SingularAttribute<SecurityGroupFailureHostVO_, String> managementNodeId;
    public static volatile SingularAttribute<SecurityGroupFailureHostVO_, Timestamp> createDate;
    public static volatile SingularAttribute<SecurityGroupFailureHostVO_, Timestamp> lastOpDate;
}
