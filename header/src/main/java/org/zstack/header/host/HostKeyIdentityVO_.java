package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostKeyIdentityVO.class)
public class HostKeyIdentityVO_ {
    public static volatile SingularAttribute<HostKeyIdentityVO, String> hostUuid;
    public static volatile SingularAttribute<HostKeyIdentityVO, String> publicKey;
    public static volatile SingularAttribute<HostKeyIdentityVO, String> fingerprint;
    public static volatile SingularAttribute<HostKeyIdentityVO, Boolean> verified;
    public static volatile SingularAttribute<HostKeyIdentityVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostKeyIdentityVO, Timestamp> lastOpDate;
}
