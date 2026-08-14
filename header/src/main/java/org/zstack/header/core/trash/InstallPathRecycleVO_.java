package org.zstack.header.core.trash;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2019/9/19.
 */
@StaticMetamodel(InstallPathRecycleVO.class)
public class InstallPathRecycleVO_ {
    public static volatile SingularAttribute<InstallPathRecycleVO, Long> trashId;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> resourceUuid;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> resourceType;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> storageUuid;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> storageType;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> installPath;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> hostUuid;
    public static volatile SingularAttribute<InstallPathRecycleVO, Boolean> isFolder;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> hypervisorType;
    public static volatile SingularAttribute<InstallPathRecycleVO, Long> size;
    public static volatile SingularAttribute<InstallPathRecycleVO, String> trashType;
    public static volatile SingularAttribute<InstallPathRecycleVO, Timestamp> createDate;
}
