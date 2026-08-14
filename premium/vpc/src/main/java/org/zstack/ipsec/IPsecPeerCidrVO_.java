package org.zstack.ipsec;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/11/8.
 */
@StaticMetamodel(IPsecPeerCidrVO.class)
public class IPsecPeerCidrVO_ {
    public static volatile SingularAttribute<IPsecPeerCidrVO, String> uuid;
    public static volatile SingularAttribute<IPsecPeerCidrVO, String> cidr;
    public static volatile SingularAttribute<IPsecPeerCidrVO, String> connectionUuid;
    public static volatile SingularAttribute<IPsecPeerCidrVO, Timestamp> createDate;
    public static volatile SingularAttribute<IPsecPeerCidrVO, Timestamp> lastOpDate;
}
