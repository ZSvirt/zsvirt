package org.zstack.header.network.l2;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(L2NetworkClusterRefVO.class)
public class L2NetworkClusterRefVO_ {
    public static volatile SingularAttribute<L2NetworkClusterRefVO, Long> id;
    public static volatile SingularAttribute<L2NetworkClusterRefVO, String> clusterUuid;
    public static volatile SingularAttribute<L2NetworkClusterRefVO, String> l2NetworkUuid;
    public static volatile SingularAttribute<L2NetworkClusterRefVO, String> l2ProviderType;
    public static volatile SingularAttribute<L2NetworkClusterRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<L2NetworkClusterRefVO, Timestamp> lastOpDate;
}
