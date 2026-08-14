package org.zstack.imagereplicator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ImageReplicationHistoryVO.class)
public class ImageReplicationHistoryVO_ {
    public static volatile SingularAttribute<ImageReplicationHistoryVO, String> backupStorageUuid;
    public static volatile SingularAttribute<ImageReplicationHistoryVO, Long> lastIndex;
    public static volatile SingularAttribute<ImageReplicationHistoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageReplicationHistoryVO, Timestamp> lastOpDate;
}
