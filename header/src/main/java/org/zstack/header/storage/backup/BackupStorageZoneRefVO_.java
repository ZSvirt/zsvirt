package org.zstack.header.storage.backup;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(BackupStorageZoneRefVO.class)
public class BackupStorageZoneRefVO_ {
    public static volatile SingularAttribute<BackupStorageZoneRefVO, String> uuid;
    public static volatile SingularAttribute<BackupStorageZoneRefVO, String> backupStorageUuid;
    public static volatile SingularAttribute<BackupStorageZoneRefVO, String> zoneUuid;
}
