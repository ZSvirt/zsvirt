package org.zstack.header.vm.metadata;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmMetadataDirtyVO.class)
public class VmMetadataDirtyVO_ {
    public static volatile SingularAttribute<VmMetadataDirtyVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmMetadataDirtyVO, String> managementNodeUuid;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Long> dirtyVersion;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Boolean> storageStructureChange;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Integer> retryCount;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Timestamp> nextRetryTime;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Timestamp> lastClaimTime;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmMetadataDirtyVO, Timestamp> lastOpDate;
}
