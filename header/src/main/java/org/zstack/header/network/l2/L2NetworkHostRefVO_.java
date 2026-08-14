package org.zstack.header.network.l2;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(L2NetworkHostRefVO.class)
public class L2NetworkHostRefVO_ {
    public static volatile SingularAttribute<L2NetworkHostRefVO, Long> id;
    public static volatile SingularAttribute<L2NetworkHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<L2NetworkHostRefVO, String> l2NetworkUuid;
    public static volatile SingularAttribute<L2NetworkHostRefVO, String> l2ProviderType;
    public static volatile SingularAttribute<L2NetworkHostRefVO, String> bridgeName;
    public static volatile SingularAttribute<L2NetworkHostRefVO, Boolean> skipDeletion;
    public static volatile SingularAttribute<L2NetworkHostRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<L2NetworkHostRefVO, Timestamp> lastOpDate;
}
