package org.zstack.network.securitygroup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SecurityGroupL3NetworkRefVO.class)
public class SecurityGroupL3NetworkRefVO_ {
    public static volatile SingularAttribute<SecurityGroupL3NetworkRefVO, String> uuid;
    public static volatile SingularAttribute<SecurityGroupL3NetworkRefVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<SecurityGroupL3NetworkRefVO, String> securityGroupUuid;
    public static volatile SingularAttribute<SecurityGroupL3NetworkRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<SecurityGroupL3NetworkRefVO, Timestamp> lastOpDate;
}
