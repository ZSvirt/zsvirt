package org.zstack.header.keyprovider;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(KmsIdentityVO.class)
public class KmsIdentityVO_ {
    public static volatile SingularAttribute<KmsIdentityVO, String> uuid;
    public static volatile SingularAttribute<KmsIdentityVO, String> kmsUuid;
    public static volatile SingularAttribute<KmsIdentityVO, KmsIdentityType> identityType;
    public static volatile SingularAttribute<KmsIdentityVO, String> clientCertPem;
    public static volatile SingularAttribute<KmsIdentityVO, String> clientKeyPem;
    public static volatile SingularAttribute<KmsIdentityVO, String> csrPem;
    public static volatile SingularAttribute<KmsIdentityVO, Timestamp> certExpiredDate;
    public static volatile SingularAttribute<KmsIdentityVO, Timestamp> createDate;
    public static volatile SingularAttribute<KmsIdentityVO, Timestamp> lastOpDate;
}
