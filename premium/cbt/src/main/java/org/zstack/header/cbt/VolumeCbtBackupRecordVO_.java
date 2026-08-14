package org.zstack.header.cbt;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VolumeCbtBackupRecordVO.class)
public class VolumeCbtBackupRecordVO_ {
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, Long> id;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> taskUuid;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> mode;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> target;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> scratchNodeName;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> bitmapName;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, String> lastBitmapName;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeCbtBackupRecordVO, Timestamp> lastOpDate;
}
