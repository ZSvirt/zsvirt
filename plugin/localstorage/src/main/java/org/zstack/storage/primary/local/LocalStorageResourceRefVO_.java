package org.zstack.storage.primary.local;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 7/1/2015.
 */
@StaticMetamodel(LocalStorageResourceRefVO.class)
public class LocalStorageResourceRefVO_ {
    public static volatile SingularAttribute<LocalStorageResourceRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<LocalStorageResourceRefVO, String> resourceUuid;
    public static volatile SingularAttribute<LocalStorageResourceRefVO, String> hostUuid;
    public static volatile SingularAttribute<LocalStorageResourceRefVO, String> size;
    public static volatile SingularAttribute<LocalStorageResourceRefVO, String> resourceType;
    public static volatile SingularAttribute<LocalStorageResourceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LocalStorageResourceRefVO, Timestamp> lastOpDate;
}
