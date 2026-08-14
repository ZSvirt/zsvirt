package org.zstack.header.storage.backup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VolumeBackupStorageRefVO.class)
public class VolumeBackupStorageRefVO_ {
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, Long> id;
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, String> volumeBackupUuid;
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, String> backupStorageUuid;
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, String> installPath;
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, VolumeBackupStatus> status;
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeBackupStorageRefVO, Timestamp> lastOpDate;
}
