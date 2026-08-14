package org.zstack.header.managementnode;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;


@StaticMetamodel(ManagementNodeVO.class)
public class ManagementNodeVO_ {
    public static volatile SingularAttribute<ManagementNodeVO, String> uuid;
    public static volatile SingularAttribute<ManagementNodeVO, String> hostName;
    public static volatile SingularAttribute<ManagementNodeVO, Integer> port;
    public static volatile SingularAttribute<ManagementNodeVO, Date> joinDate;
    public static volatile SingularAttribute<ManagementNodeVO, Date> heartBeat;
    public static volatile SingularAttribute<ManagementNodeVO, ManagementNodeState> state;
}
