package org.zstack.imagereplicator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ImageReplicationGroupBackupStorageRefVO.class)
public class ImageReplicationGroupBackupStorageRefVO_ {
    public static volatile SingularAttribute<ImageReplicationGroupBackupStorageRefVO, String> replicationGroupUuid;
    public static volatile SingularAttribute<ImageReplicationGroupBackupStorageRefVO, String> backupStorageUuid;
    public static volatile SingularAttribute<ImageReplicationGroupBackupStorageRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageReplicationGroupBackupStorageRefVO, Timestamp> lastOpDate;
}
