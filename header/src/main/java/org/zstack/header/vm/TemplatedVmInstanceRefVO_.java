package org.zstack.header.vm;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(TemplatedVmInstanceRefVO.class)
public class TemplatedVmInstanceRefVO_ {
    public static volatile SingularAttribute<TemplatedVmInstanceRefVO, Long> id;
    public static volatile SingularAttribute<TemplatedVmInstanceRefVO, String> templatedVmInstanceUuid;
    public static volatile SingularAttribute<TemplatedVmInstanceRefVO, String> vmInstanceUuid;
}
