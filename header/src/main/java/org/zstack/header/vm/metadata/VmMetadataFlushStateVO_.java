package org.zstack.header.vm.metadata;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmMetadataFlushStateVO.class)
public class VmMetadataFlushStateVO_ {
    public static volatile SingularAttribute<VmMetadataFlushStateVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmMetadataFlushStateVO, Timestamp> lastFlushFinishTime;
    public static volatile SingularAttribute<VmMetadataFlushStateVO, Boolean> pendingStaleRecovery;
    public static volatile SingularAttribute<VmMetadataFlushStateVO, Integer> staleRecoveryCount;
    public static volatile SingularAttribute<VmMetadataFlushStateVO, String> metadataSnapshot;
    public static volatile SingularAttribute<VmMetadataFlushStateVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmMetadataFlushStateVO, Timestamp> lastOpDate;
}
