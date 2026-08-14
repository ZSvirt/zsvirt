package org.zstack.header.vm.devices;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceResourceMetadataArchiveVO.class)
public class VmInstanceResourceMetadataArchiveVO_ {
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, Long> id;
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, String> resourceUuid;
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, String> addressGroupUuid;
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, String> metadata;
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, String> metadataClass;
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, String> vmInstanceUuid;
}
