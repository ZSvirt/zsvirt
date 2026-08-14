package org.zstack.header.storage.backup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VolumeBackupHistoryVO.class)
public class VolumeBackupHistoryVO_ {
    public static volatile SingularAttribute<VolumeBackupVO, String> uuid;
    public static volatile SingularAttribute<VolumeBackupVO, String> lastBackupUuid;
    public static volatile SingularAttribute<VolumeBackupVO, String> bitmap;
    public static volatile SingularAttribute<VolumeBackupVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeBackupVO, Timestamp> lastOpDate;
}
