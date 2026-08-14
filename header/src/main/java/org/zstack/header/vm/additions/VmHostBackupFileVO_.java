package org.zstack.header.vm.additions;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmHostBackupFileVO.class)
public class VmHostBackupFileVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmHostBackupFileVO, String> resourceUuid;
    public static volatile SingularAttribute<VmHostBackupFileVO, VmHostFileType> type;
    public static volatile SingularAttribute<VmHostBackupFileVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmHostBackupFileVO, Timestamp> lastOpDate;
}
