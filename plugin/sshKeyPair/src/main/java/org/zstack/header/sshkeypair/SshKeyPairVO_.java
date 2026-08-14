package org.zstack.header.sshkeypair;

import org.zstack.directory.DirectoryVO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SshKeyPairVO.class)
public class SshKeyPairVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SshKeyPairVO, String> name;
    public static volatile SingularAttribute<SshKeyPairVO, String> description;
    public static volatile SingularAttribute<SshKeyPairVO, String> publicKey;
    public static volatile SingularAttribute<SshKeyPairVO, Timestamp> createDate;
    public static volatile SingularAttribute<SshKeyPairVO, Timestamp> lastOpDate;
}
