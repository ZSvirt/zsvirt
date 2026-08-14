package org.zstack.ha.hostHastate;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2023/4/14
 */
@StaticMetamodel(HostHaStateVO.class)
public class HostHaStateVO_ {
    public static volatile SingularAttribute<HostHaStateVO, String> uuid;
    public static volatile SingularAttribute<HostHaStateVO, HostHaState> state;
    public static volatile SingularAttribute<HostHaStateVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostHaStateVO, Timestamp> lastOpDate;
}
