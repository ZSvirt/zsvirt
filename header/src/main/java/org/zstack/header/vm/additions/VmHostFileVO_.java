package org.zstack.header.vm.additions;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmHostFileVO.class)
public class VmHostFileVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmHostFileVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmHostFileVO, String> hostUuid;
    public static volatile SingularAttribute<VmHostFileVO, VmHostFileType> type;
    public static volatile SingularAttribute<VmHostFileVO, String> path;
    public static volatile SingularAttribute<VmHostFileVO, String> lastSyncReason;
    public static volatile SingularAttribute<VmHostFileVO, Timestamp> changeDate;
    public static volatile SingularAttribute<VmHostFileVO, Timestamp> lastSyncDate;
    public static volatile SingularAttribute<VmHostFileVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmHostFileVO, Timestamp> lastOpDate;
}
