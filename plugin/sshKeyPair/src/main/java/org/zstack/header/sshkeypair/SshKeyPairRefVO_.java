package org.zstack.header.sshkeypair;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SshKeyPairVO.class)
public class SshKeyPairRefVO_ {
    public static volatile SingularAttribute<SshKeyPairRefVO, Long> id;
    public static volatile SingularAttribute<SshKeyPairRefVO, String> sshKeyPairUuid;
    public static volatile SingularAttribute<SshKeyPairRefVO, String> resourceUuid;
    public static volatile SingularAttribute<SshKeyPairRefVO, String> resourceType;
    public static volatile SingularAttribute<SshKeyPairRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<SshKeyPairRefVO, Timestamp> lastOpDate;
}
