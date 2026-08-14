package org.zstack.header.keyprovider;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(KmsVO.class)
public class KmsVO_ extends KeyProviderVO_ {
    public static volatile SingularAttribute<KmsVO, String> endpoint;
    public static volatile SingularAttribute<KmsVO, Integer> port;
    public static volatile SingularAttribute<KmsVO, KmipVersion> kmipVersion;
    public static volatile SingularAttribute<KmsVO, String> username;
    public static volatile SingularAttribute<KmsVO, String> password;
    public static volatile SingularAttribute<KmsVO, KmsTrustState> trustState;
    public static volatile SingularAttribute<KmsVO, String> activeIdentityUuid;
    public static volatile SingularAttribute<KmsVO, Timestamp> serverCertExpiredDate;
    public static volatile SingularAttribute<KmsVO, String> serverCertPem;
}
