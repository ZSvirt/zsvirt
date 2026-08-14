package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017-05-08.
 */
@StaticMetamodel(PrimaryStorageHostRefVO.class)
public class PrimaryStorageHostRefVO_ {
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, Long> id;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, PrimaryStorageHostStatus> status;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, Timestamp> lastOpDate;
}
