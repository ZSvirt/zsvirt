package org.zstack.header.keyprovider;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(EncryptedResourceKeyRefVO.class)
public class EncryptedResourceKeyRefVO_ {
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, Long> id;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> resourceType;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> resourceUuid;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> providerUuid;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> providerName;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, Integer> keyVersion;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> kekRef;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> wrappedDek;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, String> algorithm;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<EncryptedResourceKeyRefVO, Timestamp> lastOpDate;
}
