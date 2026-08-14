package org.zstack.header.vpc;

import org.zstack.header.vpc.VpcSnatStateVO;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;


/**
 */
@StaticMetamodel(VpcSnatStateVO.class)
public class VpcSnatStateVO_ {
    public static volatile SingularAttribute<VpcSnatStateVO, String> uuid;
    public static volatile SingularAttribute<VpcSnatStateVO, String> vpcUuid;
    public static volatile SingularAttribute<VpcSnatStateVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<VpcSnatStateVO, String> state;
    public static volatile SingularAttribute<VpcSnatStateVO, Timestamp> createDate;
    public static volatile SingularAttribute<VpcSnatStateVO, Timestamp> lastOpDate;
}
