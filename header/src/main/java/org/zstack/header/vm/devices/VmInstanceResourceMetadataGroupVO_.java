package org.zstack.header.vm.devices;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceResourceMetadataGroupVO.class)
public class VmInstanceResourceMetadataGroupVO_ {
    public static volatile SingularAttribute<VmInstanceResourceMetadataGroupVO, String> uuid;
    public static volatile SingularAttribute<VmInstanceResourceMetadataGroupVO, String> resourceUuid;
    public static volatile SingularAttribute<VmInstanceResourceMetadataArchiveVO, String> vmInstanceUuid;
}
