package org.zstack.storage.backup.imagestore;

import org.zstack.header.storage.backup.BackupStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ImageStoreBackupStorageVO.class)
public class ImageStoreBackupStorageVO_ extends BackupStorageVO_ {
    public static volatile SingularAttribute<ImageStoreBackupStorageVO, String> hostname;
    public static volatile SingularAttribute<ImageStoreBackupStorageVO, Integer> sshPort;
}
