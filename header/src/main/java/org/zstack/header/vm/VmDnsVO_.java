package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmDnsVO.class)
public class VmDnsVO_ {
    public static volatile SingularAttribute<VmDnsVO, Long> id;
    public static volatile SingularAttribute<VmDnsVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmDnsVO, String> vmNicUuid;
    public static volatile SingularAttribute<VmDnsVO, String> dns;
    public static volatile SingularAttribute<VmDnsVO, Integer> ipVersion;
    public static volatile SingularAttribute<VmDnsVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmDnsVO, Timestamp> lastOpDate;
}
