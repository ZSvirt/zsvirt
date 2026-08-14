package org.zstack.header.host;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostNumaNodeVO.class)
public class HostNumaNodeVO_ {
    public static volatile SingularAttribute<HostNumaNodeVO, Long> id;
    public static volatile SingularAttribute<HostNumaNodeVO, String> hostUuid;
    public static volatile SingularAttribute<HostNumaNodeVO, Integer> nodeID;
    public static volatile SingularAttribute<HostNumaNodeVO, String> nodeCPUs;
    public static volatile SingularAttribute<HostNumaNodeVO, Long> nodeMemSize;
    public static volatile SingularAttribute<HostNumaNodeVO, String> nodeDistance;
}
