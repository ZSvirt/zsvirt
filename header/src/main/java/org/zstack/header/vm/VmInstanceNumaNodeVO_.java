package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmInstanceNumaNodeVO.class)
public class VmInstanceNumaNodeVO_ {
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, Long> id;
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, String> vmUuid;
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, Integer> vNodeID;
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, String> vNodeCPUs;
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, Long> vNodeMemeSize;
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, String> vNodeDistance;
    public static volatile SingularAttribute<VmInstanceNumaNodeVO, Integer> pNodeID;

}
