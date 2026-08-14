package org.zstack.header.storage.backup;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.VolumeType;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VolumeBackupVO.class)
public class VolumeBackupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeBackupVO, String> name;
    public static volatile SingularAttribute<VolumeBackupVO, String> description;
    public static volatile SingularAttribute<VolumeBackupVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeBackupVO, Long> size;
    public static volatile SingularAttribute<VolumeBackupVO, VolumeType> type;
    public static volatile SingularAttribute<VolumeBackupVO, VolumeBackupState> state;
    public static volatile SingularAttribute<VolumeBackupVO, VolumeBackupStatus> status;
    public static volatile SingularAttribute<VolumeBackupVO, String> metadata;
    public static volatile SingularAttribute<VolumeBackupVO, String> groupUuid;
    public static volatile SingularAttribute<VolumeBackupVO, BackupMode> mode;
    public static volatile SingularAttribute<VolumeBackupVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VolumeBackupVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeBackupVO, Timestamp> lastOpDate;
}
