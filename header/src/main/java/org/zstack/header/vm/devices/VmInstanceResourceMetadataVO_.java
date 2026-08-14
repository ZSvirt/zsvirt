package org.zstack.header.vm.devices;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceResourceMetadataVO.class)
public class VmInstanceResourceMetadataVO_ {
    public static volatile SingularAttribute<VmInstanceResourceMetadataVO, Long> id;
    public static volatile SingularAttribute<VmInstanceResourceMetadataVO, String> resourceUuid;
    public static volatile SingularAttribute<VmInstanceResourceMetadataVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmInstanceResourceMetadataVO, String> deviceAddress;
    public static volatile SingularAttribute<VmInstanceResourceMetadataVO, String> metadata;
    public static volatile SingularAttribute<VmInstanceResourceMetadataVO, String> metadataClass;
}
