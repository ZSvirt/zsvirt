package org.zstack.header.image;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(ImageBackupStorageRefVO.class)
public class ImageBackupStorageRefVO_ {
    public static volatile SingularAttribute<ImageBackupStorageRefVO, Long> id;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, String> imageUuid;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, String> backupStorageUuid;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, String> installPath;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, ImageStatus> status;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, String> exportMd5Sum;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, String> exportUrl;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageBackupStorageRefVO, Timestamp> lastOpDate;
}
